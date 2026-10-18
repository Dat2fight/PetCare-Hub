package com.petcarehub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component @RequiredArgsConstructor
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    public static final String NOTIFICATIONS = "/user/queue/notifications";
    private final JwtTokenProvider tokens;
    private final UserDetailsServiceImpl users;
    private final Map<String, SessionIdentity> sessions = new ConcurrentHashMap<>();
    private record SessionIdentity(Long userId, String username, Instant expiresAt) {}

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null || headers.getCommand() == null) return message; // heartbeat
        StompCommand command = headers.getCommand();
        String sessionId = headers.getSessionId();
        if (sessionId == null) throw denied();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            String authorization = headers.getFirstNativeHeader("Authorization");
            headers.removeNativeHeader("Authorization"); // Never retain a credential in broker/error frames.
            if (sessions.containsKey(sessionId) || authorization == null || !authorization.startsWith("Bearer ")) throw denied();
            String jwt = authorization.substring(7);
            try {
                if (!tokens.validateToken(jwt)) throw denied();
                var principal = (UserDetailsImpl) users.loadUserByUsername(tokens.getUsernameFromJWT(jwt));
                if (!principal.isEnabled()) throw denied();
                Instant expiresAt = tokens.getExpirationFromJWT(jwt).toInstant();
                if (!expiresAt.isAfter(Instant.now())) throw denied();
                headers.setUser(new UsernamePasswordAuthenticationToken(principal.getId().toString(), null, principal.getAuthorities()));
                sessions.put(sessionId, new SessionIdentity(principal.getId(), principal.getUsername(), expiresAt));
                return message;
            } catch (RuntimeException ex) {
                throw denied();
            }
        }
        if (command == StompCommand.DISCONNECT) {
            sessions.remove(sessionId);
            return message;
        }
        SessionIdentity identity = sessions.get(sessionId);
        if (!valid(identity) || headers.getUser() == null || !identity.userId().toString().equals(headers.getUser().getName())) throw denied();
        if (command == StompCommand.SUBSCRIBE && NOTIFICATIONS.equals(headers.getDestination())) return message;
        if (command == StompCommand.UNSUBSCRIBE || command == StompCommand.ACK || command == StompCommand.NACK) return message;
        // This socket is notification-only: clients cannot SEND to app, broker, or another user.
        throw denied();
    }

    public ChannelInterceptor outboundInterceptor() {
        return new ChannelInterceptor() {
            @Override public Message<?> preSend(Message<?> message, MessageChannel channel) {
                if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders()) != SimpMessageType.MESSAGE) return message;
                String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
                return sessionId != null && valid(sessions.get(sessionId)) ? message : null;
            }
        };
    }

    private boolean valid(SessionIdentity identity) {
        if (identity == null || !identity.expiresAt().isAfter(Instant.now())) return false;
        try {
            var current = (UserDetailsImpl) users.loadUserByUsername(identity.username());
            return current.isEnabled() && current.getId().equals(identity.userId());
        } catch (RuntimeException ex) { return false; }
    }

    @EventListener public void disconnected(SessionDisconnectEvent event) { sessions.remove(event.getSessionId()); }
    private AccessDeniedException denied() { return new AccessDeniedException("WebSocket authentication or destination denied"); }
}
