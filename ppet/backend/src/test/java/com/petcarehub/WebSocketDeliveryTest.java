package com.petcarehub;

import com.petcarehub.entity.Notification;
import com.petcarehub.entity.User;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.JwtTokenProvider;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebSocketDeliveryTest {
    @LocalServerPort int port;
    @Autowired JwtTokenProvider tokens;
    @Autowired WebSocketNotificationService notifications;
    @MockBean UserRepository users;

    User account(long id) {
        User user = User.builder().username("socket-"+id).email("socket-"+id+"@example.test").build();
        user.setId(id);
        when(users.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        return user;
    }

    StompSession connect(WebSocketStompClient client, User user) throws Exception {
        var principal = UserDetailsImpl.build(user);
        var connect = new StompHeaders();
        connect.add("Authorization", "Bearer " + tokens.generateToken(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
        var handshake = new WebSocketHttpHeaders();
        handshake.setOrigin("http://localhost:5173");
        return client.connectAsync("ws://localhost:"+port+"/ws", handshake, connect,
                new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
    }

    StompFrameHandler receive(BlockingQueue<String> queue) {
        return new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return byte[].class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                queue.add(new String((byte[]) payload, StandardCharsets.UTF_8));
            }
        };
    }

    @Test void realSockJsBrokerDeliversOnlyToAuthenticatedRecipient() throws Exception {
        var client = new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        // Inspect JSON frames as bytes, regardless of their application/json content type.
        client.setMessageConverter(new SimpleMessageConverter());
        client.setDefaultHeartbeat(new long[]{0, 0});
        User first = account(701L), second = account(702L);
        BlockingQueue<String> firstInbox = new LinkedBlockingQueue<>(), secondInbox = new LinkedBlockingQueue<>();
        StompSession one = null, two = null;
        try {
            one = connect(client, first); two = connect(client, second);
            one.subscribe("/user/queue/notifications", receive(firstInbox));
            two.subscribe("/user/queue/notifications", receive(secondInbox));
            // Retry until the asynchronous subscriptions have reached the broker.
            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                notifications.sendNotification(first.getId(), Notification.builder().user(first).message("first-only").build());
                notifications.sendNotification(second.getId(), Notification.builder().user(second).message("second-only").build());
                assertFalse(firstInbox.isEmpty()); assertFalse(secondInbox.isEmpty());
            });
            // Retain every frame: retrying must never discard a cross-user delivery.
            assertTrue(firstInbox.stream().allMatch(value -> value.contains("first-only")));
            assertTrue(secondInbox.stream().allMatch(value -> value.contains("second-only")));
        } finally {
            if (one != null && one.isConnected()) one.disconnect();
            if (two != null && two.isConnected()) two.disconnect();
            client.stop();
        }
    }
}
