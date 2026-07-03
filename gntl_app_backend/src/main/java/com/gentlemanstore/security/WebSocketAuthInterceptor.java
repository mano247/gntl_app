package com.gentlemanstore.security;

import com.gentlemanstore.support.model.SupportTicket;
import com.gentlemanstore.support.repository.SupportTicketRepository;
import com.gentlemanstore.user.model.User;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Autentifikacija i autorizacija za WebSocket kanal:
 * - HandshakeInterceptor: validira JWT (query parametar "token" ili "Authorization" header)
 *   pre uspostavljanja konekcije; bez validnog tokena handshake se odbija sa 401.
 * - ChannelInterceptor: na SUBSCRIBE proverava da /topic/chat/{sessionId} pripada
 *   pozivaocu (vlasnik tiketa ili staff) — isti ownership obrazac kao SupportService.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor, ChannelInterceptor {

    public static final String AUTH_ATTRIBUTE = "wsAuthentication";
    private static final Pattern CHAT_TOPIC_PATTERN = Pattern.compile("^/topic/chat/(\\d+)$");
    private static final Pattern USER_UNREAD_TOPIC_PATTERN = Pattern.compile("^/topic/user/(\\d+)/unread$");
    private static final String EMPLOYEE_TOPIC_PREFIX = "/topic/employee/";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final SupportTicketRepository supportTicketRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = resolveToken(request);
        if (token == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(AUTH_ATTRIBUTE, new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()));
            return true;
        } catch (JwtException | UsernameNotFoundException | IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination == null) {
                return message;
            }
            Matcher matcher = CHAT_TOPIC_PATTERN.matcher(destination);
            if (matcher.matches()) {
                Long sessionId = Long.valueOf(matcher.group(1));
                User user = extractUser(accessor.getUser());
                if (user == null || !canAccessSession(sessionId, user)) {
                    throw new MessageDeliveryException("Access denied to " + destination);
                }
            }

            // Badge topici — isti ownership obrazac kao chat:
            // /topic/user/{id}/unread sme da sluša samo taj korisnik,
            // /topic/employee/** samo staff.
            Matcher unreadMatcher = USER_UNREAD_TOPIC_PATTERN.matcher(destination);
            if (unreadMatcher.matches()) {
                Long userId = Long.valueOf(unreadMatcher.group(1));
                User user = extractUser(accessor.getUser());
                if (user == null || !user.getId().equals(userId)) {
                    throw new MessageDeliveryException("Access denied to " + destination);
                }
            }
            if (destination.startsWith(EMPLOYEE_TOPIC_PREFIX)) {
                User user = extractUser(accessor.getUser());
                if (user == null || !isStaff(user)) {
                    throw new MessageDeliveryException("Access denied to " + destination);
                }
            }
        }
        return message;
    }

    private User extractUser(Principal principal) {
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private boolean canAccessSession(Long sessionId, User user) {
        SupportTicket ticket = supportTicketRepository.findByChatSessionId(sessionId).orElse(null);
        if (ticket == null) {
            return false;
        }
        return isStaff(user) || ticket.getUser().getId().equals(user.getId());
    }

    private boolean isStaff(User user) {
        return user.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_ADMIN")
                        || authority.getAuthority().equals("ROLE_MANAGER")
                        || authority.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    private String resolveToken(ServerHttpRequest request) {
        String queryToken = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
