package dev.infreej.jeckathon.gateway.ws;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

// 모든 웹소켓 메시지의 입구. JSON의 "type"으로 WsMessageHandler를 골라 넘긴다.
// 잘못된 메시지는 error로 응답하고 연결은 유지한다.
@Slf4j
@Component
public class GatewayWebSocketHandler extends TextWebSocketHandler {

	private final Map<String, WsMessageHandler> handlers;
	private final WsSender sender;
	private final ObjectMapper objectMapper;

	public GatewayWebSocketHandler(List<WsMessageHandler> handlers, WsSender sender, ObjectMapper objectMapper) {
		// type이 겹치면 Collectors.toMap이 IllegalStateException을 던져 앱이 뜨지 않는다.
		this.handlers = handlers.stream().collect(Collectors.toMap(WsMessageHandler::type, Function.identity()));
		this.sender = sender;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) {
		JsonNode payload;
		try {
			payload = objectMapper.readTree(message.getPayload());
		} catch (JacksonException e) {
			sender.sendError(session, "JSON 형식이 올바르지 않습니다.");
			return;
		}
		JsonNode type = payload.get("type");
		if (!payload.isObject() || type == null || !type.isString()) {
			sender.sendError(session, "type이 필요합니다.");
			return;
		}
		WsMessageHandler handler = handlers.get(type.asString());
		if (handler == null) {
			sender.sendError(session, "지원하지 않는 type입니다: " + type.asString());
			return;
		}
		try {
			handler.handle(session, payload);
		} catch (RuntimeException e) {
			log.error("웹소켓 메시지 처리 실패 type={} session={}", type.asString(), session.getId(), e);
			sender.sendError(session, "요청을 처리하지 못했습니다.");
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sender.unregister(session);
	}
}
