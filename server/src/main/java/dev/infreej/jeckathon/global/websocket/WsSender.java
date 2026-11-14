package dev.infreej.jeckathon.global.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// 세션에 JSON을 보낸다. 같은 세션에 여러 스레드(처리기, 시세 스케줄러)가 동시에 보내도 되도록 세션마다 데코레이터를 하나씩 둔다.
@Slf4j
@Component
public class WsSender {

	private static final int SEND_TIME_LIMIT_MS = 10_000;
	private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
	private final ObjectMapper objectMapper;

	public WsSender(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public void send(WebSocketSession session, Object message) {
		// 닫힌 세션을 다시 등록하면 unregister 뒤에 데코레이터가 남는다.
		if (!session.isOpen()) {
			return;
		}
		WebSocketSession target = sessions.computeIfAbsent(session.getId(), id -> decorate(session));
		if (!target.isOpen()) {
			return;
		}
		try {
			target.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
		} catch (IOException | RuntimeException e) {
			log.warn("웹소켓 전송 실패 session={}: {}", session.getId(), e.getMessage());
		}
	}

	public void sendError(WebSocketSession session, String message) {
		send(session, new ErrorMessage("error", message));
	}

	void unregister(WebSocketSession session) {
		sessions.remove(session.getId());
	}

	private static WebSocketSession decorate(WebSocketSession session) {
		return new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES);
	}

	record ErrorMessage(String type, String message) {
	}
}
