package com.trojanscheduler.websocket;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.trojanscheduler.auth.JwtService;

/**
 * STOMP over WebSocket. Clients connect to /ws, authenticate via JWT in CONNECT header,
 * subscribe to /user/queue/notifications for real-time seat alerts.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private static final String DESTINATION_PREFIX_APP = "/app";
	private static final String BROKER_PREFIX_QUEUE = "/queue";
	private static final String BROKER_PREFIX_TOPIC = "/topic";

	private final JwtService jwtService;

	public WebSocketConfig(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
				.setAllowedOrigins("http://localhost:5173", "http://localhost:3000")
				.withSockJS();
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes(DESTINATION_PREFIX_APP);
		config.enableSimpleBroker(BROKER_PREFIX_QUEUE, BROKER_PREFIX_TOPIC);
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
					String token = tokenFromHeaders(accessor);
					if (token != null) {
						try {
							var claims = jwtService.parseAccessToken(token);
							String userId = claims.getSubject();
							accessor.setUser(new StompPrincipal(userId));
						} catch (Exception ignored) {
							// invalid token; user stays anonymous
						}
					}
				}
				return message;
			}
		});
	}

	private static String tokenFromHeaders(StompHeaderAccessor accessor) {
		List<String> auth = accessor.getNativeHeader("Authorization");
		if (auth != null && !auth.isEmpty()) {
			String v = auth.get(0);
			if (v != null && v.startsWith("Bearer ")) return v.substring(7).trim();
		}
		return null;
	}

	/** Principal whose name is userId (for convertAndSendToUser). */
	public static final class StompPrincipal implements java.security.Principal {
		private final String userId;

		public StompPrincipal(String userId) {
			this.userId = userId;
		}

		@Override
		public String getName() {
			return userId;
		}
	}
}
