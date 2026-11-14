package dev.infreej.jeckathon.gateway.ws;

import org.springframework.web.socket.WebSocketSession;

import tools.jackson.databind.JsonNode;

// 웹소켓 메시지의 "type" 하나를 처리한다. 빈으로 등록하면 GatewayWebSocketHandler가 자동으로 찾는다.
public interface WsMessageHandler {

	String type();

	void handle(WebSocketSession session, JsonNode payload);
}
