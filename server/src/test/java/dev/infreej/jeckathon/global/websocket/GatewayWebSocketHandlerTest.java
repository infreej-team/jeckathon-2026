package dev.infreej.jeckathon.global.websocket;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.databind.JsonNode;

class GatewayWebSocketHandlerTest {
	List<JsonNode> received = new ArrayList<>();
	WsMessageHandler echo = new WsMessageHandler() {
		public String type() { return "echo"; }
		public void handle(WebSocketSession s, JsonNode p) {
			if (p.has("boom")) throw new IllegalStateException("boom");
			received.add(p);
		}
	};
	GatewayWebSocketHandler gateway = new GatewayWebSocketHandler(List.of(echo), List.of(), new WsSender(MAPPER), MAPPER);
	WebSocketSession session = session("s1", "u1");

	@Test
	void type으로_처리기를_골라_넘긴다() throws Exception {
		gateway.handleMessage(session, new TextMessage("{\"type\":\"echo\",\"x\":1}"));

		assertThat(received).containsExactly(json("{\"type\":\"echo\",\"x\":1}"));
		assertThat(sent(session)).isEmpty();
	}

	@Test
	void 잘못된_메시지는_error로_응답하고_연결을_유지한다() throws Exception {
		for (String raw : List.of("{not json", "[1,2]", "{\"x\":1}", "{\"type\":3}", "{\"type\":\"nope\"}", "{\"type\":\"echo\",\"boom\":true}")) {
			gateway.handleMessage(session, new TextMessage(raw));
		}

		assertThat(sent(session)).hasSize(6).allSatisfy(n -> assertThat(n.get("type").asString()).isEqualTo("error"));
		assertThat(received).isEmpty();
		verify(session, never()).close();
		verify(session, never()).close(any());
	}

	@Test
	void type이_겹치는_처리기가_있으면_시작하지_않는다() {
		assertThatThrownBy(() -> new GatewayWebSocketHandler(List.of(echo, echo), List.of(), new WsSender(MAPPER), MAPPER))
				.isInstanceOf(IllegalStateException.class);
	}
}
