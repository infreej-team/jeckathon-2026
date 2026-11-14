package dev.infreej.jeckathon.gateway.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

	// nginx는 /api/ 아래만 server로 넘기고 Upgrade 헤더도 그 경로에 설정되어 있다. /ws로 두면 client로 간다.
	public static final String ENDPOINT = "/api/ws";

	private final GatewayWebSocketHandler gatewayHandler;
	private final String[] allowedOrigins;

	public WebSocketConfig(GatewayWebSocketHandler gatewayHandler,
			@Value("${app.websocket.allowed-origins}") String[] allowedOrigins) {
		this.gatewayHandler = gatewayHandler;
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(gatewayHandler, ENDPOINT)
				.addInterceptors(new UserHandshakeInterceptor())
				.setAllowedOriginPatterns(allowedOrigins);
	}
}
