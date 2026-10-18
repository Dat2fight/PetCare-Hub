package com.petcarehub;

import com.petcarehub.config.WebSocketConfig;
import com.petcarehub.entity.Notification;
import com.petcarehub.entity.User;
import com.petcarehub.security.*;
import com.petcarehub.service.WebSocketNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StompAuthorizationTest {
    JwtTokenProvider tokens = mock(JwtTokenProvider.class);
    UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
    StompAuthorizationInterceptor interceptor = new StompAuthorizationInterceptor(tokens, users);
    String token = UUID.randomUUID().toString();
    UserDetailsImpl principal(boolean enabled) {
        return new UserDetailsImpl(1L,"customer","c@example.test","unused",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")),enabled);
    }
    Message<byte[]> message(StompHeaderAccessor headers) {
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0],headers.getMessageHeaders());
    }
    StompHeaderAccessor connect() {
        when(tokens.validateToken(token)).thenReturn(true);
        when(tokens.getUsernameFromJWT(token)).thenReturn("customer");
        when(tokens.getExpirationFromJWT(token)).thenReturn(Date.from(Instant.now().plusSeconds(60)));
        when(users.loadUserByUsername("customer")).thenReturn(principal(true));
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("session-one"); headers.setNativeHeader("Authorization","Bearer "+token);
        interceptor.preSend(message(headers),null);
        return headers;
    }
    StompHeaderAccessor frame(StompCommand command, String destination, StompHeaderAccessor connected) {
        var headers=StompHeaderAccessor.create(command); headers.setSessionId("session-one");
        headers.setUser(connected.getUser()); headers.setDestination(destination); return headers;
    }
    @Test void anonymousAndInvalidConnectAreRejected() {
        var headers=StompHeaderAccessor.create(StompCommand.CONNECT); headers.setSessionId("anonymous");
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(headers),null));
        headers.setNativeHeader("Authorization","Bearer "+token);
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(headers),null));
        assertNull(headers.getFirstNativeHeader("Authorization"));
    }
    @Test void expiredOrDisabledAccountCannotConnect() {
        when(tokens.validateToken(token)).thenReturn(true);
        when(tokens.getUsernameFromJWT(token)).thenReturn("customer");
        when(users.loadUserByUsername("customer")).thenReturn(principal(false));
        var headers=StompHeaderAccessor.create(StompCommand.CONNECT); headers.setSessionId("blocked");
        headers.setNativeHeader("Authorization","Bearer "+token);
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(headers),null));
        when(users.loadUserByUsername("customer")).thenReturn(principal(true));
        when(tokens.getExpirationFromJWT(token)).thenReturn(Date.from(Instant.now().minusSeconds(1)));
        headers.setNativeHeader("Authorization","Bearer "+token);
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(headers),null));
    }
    @Test void authenticatedCustomerCanOnlySubscribeToTheirUserQueue() {
        var connected=connect(); assertEquals("1",connected.getUser().getName());
        assertNull(connected.getFirstNativeHeader("Authorization"));
        assertNotNull(interceptor.preSend(message(frame(StompCommand.SUBSCRIBE,"/user/queue/notifications",connected)),null));
    }
    @ParameterizedTest @ValueSource(strings={"/topic/notifications/2","/topic/notifications/1","/user/2/queue/notifications","/queue/notifications-userother","/user/queue/notifications/extra","/topic/**"})
    void cannotSubscribeToAnotherUserOrRawBrokerDestination(String destination) {
        var connected=connect();
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(frame(StompCommand.SUBSCRIBE,destination,connected)),null));
    }
    @ParameterizedTest @ValueSource(strings={"/app/notifications","/queue/notifications","/user/queue/notifications","/topic/notifications/2"})
    void clientsCannotPublishNotifications(String destination) {
        var connected=connect();
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(frame(StompCommand.SEND,destination,connected)),null));
    }
    @Test void disablingAccountBlocksExistingSessionInboundAndOutbound() {
        var connected=connect();
        var outbound=SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE); outbound.setSessionId("session-one");
        Message<?> message=MessageBuilder.createMessage(new byte[0],outbound.getMessageHeaders());
        assertNotNull(interceptor.outboundInterceptor().preSend(message,null));
        when(users.loadUserByUsername("customer")).thenReturn(principal(false));
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(frame(StompCommand.SUBSCRIBE,"/user/queue/notifications",connected)),null));
        assertNull(interceptor.outboundInterceptor().preSend(message,null));
    }
    @Test void disconnectRemovesSessionAuthorization() {
        var connected=connect();
        interceptor.preSend(message(frame(StompCommand.DISCONNECT,"",connected)),null);
        assertThrows(AccessDeniedException.class,()->interceptor.preSend(message(frame(StompCommand.SUBSCRIBE,"/user/queue/notifications",connected)),null));
    }
    @Test void notificationUsesUserDestinationAndRejectsMismatchedRecipient() {
        var messaging=mock(SimpMessagingTemplate.class);
        var service=new WebSocketNotificationService(messaging);
        var account=new User();account.setId(1L);
        var notice=Notification.builder().user(account).message("Test").build();
        service.sendNotification(1L,notice);
        verify(messaging).convertAndSendToUser(eq("1"),eq("/queue/notifications"),any(Object.class));
        assertThrows(AccessDeniedException.class,()->service.sendNotification(2L,notice));
    }
    @Test void wildcardOriginsFailClosed() {
        var config=new WebSocketConfig(interceptor);
        ReflectionTestUtils.setField(config,"allowedOrigins","*");
        assertThrows(IllegalStateException.class,()->config.registerStompEndpoints(mock(StompEndpointRegistry.class)));
    }
}
