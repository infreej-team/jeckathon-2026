package dev.infreej.jeckathon.global.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public final class FakeSessions {
	public static final ObjectMapper MAPPER = JsonMapper.builder().build();

	public static WebSocketSession session(String id, String userId) {
		WebSocketSession s = mock(WebSocketSession.class);
		Map<String, Object> attrs = new HashMap<>();
		attrs.put(UserHandshakeInterceptor.USER_ID, userId);
		when(s.getId()).thenReturn(id);
		when(s.isOpen()).thenReturn(true);
		when(s.getAttributes()).thenReturn(attrs);
		return s;
	}

	public static List<JsonNode> sent(WebSocketSession s) throws Exception {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<WebSocketMessage<?>> c = ArgumentCaptor.forClass(WebSocketMessage.class);
		verify(s, atLeast(0)).sendMessage(c.capture());
		return c.getAllValues().stream().map(m -> MAPPER.readTree(((TextMessage) m).getPayload())).toList();
	}

	public static JsonNode json(String raw) {
		return MAPPER.readTree(raw);
	}
}
