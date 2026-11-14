package dev.infreej.jeckathon.domain.trade.ws;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.trade.TradeCount;
import dev.infreej.jeckathon.domain.trade.TradeService;
import dev.infreej.jeckathon.global.websocket.WsSender;
import tools.jackson.databind.JsonNode;

class TradeMessageHandlerTest {
	TradeService trade = mock(TradeService.class);
	TradeMessageHandler handler = new TradeMessageHandler(trade, new WsSender(MAPPER));
	WebSocketSession session = session("s1", "u1");

	@Test
	void 정상_메시지는_세션의_userId로_반영하고_반환값을_ack로_보낸다() throws Exception {
		when(trade.applyTrade("room-1", "u1", new TradeCount(5, 1))).thenReturn(new TradeCount(3, 1));

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"room-1\",\"buy\":5,\"sell\":1}"));

		assertThat(sent(session)).containsExactly(json("{\"type\":\"trade.ack\",\"roomId\":\"room-1\",\"buy\":3,\"sell\":1}"));
	}

	@Test
	void roomId가_없거나_비면_error이고_반영하지_않는다() throws Exception {
		handler.handle(session, json("{\"type\":\"trade\",\"buy\":1}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"  \",\"buy\":1}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":7,\"buy\":1}"));

		assertThat(sent(session)).extracting(n -> n.get("type").asString()).containsExactly("error", "error", "error");
		verifyNoInteractions(trade);
	}

	@Test
	void 정수가_아닌_buy_sell은_0으로_넘긴다() throws Exception {
		when(trade.applyTrade(any(), any(), any())).thenReturn(TradeCount.ZERO);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"r\",\"buy\":\"2\",\"sell\":1.5}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"r\",\"buy\":99999999999,\"sell\":-2}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"r\"}"));

		verify(trade).applyTrade("r", "u1", new TradeCount(0, -2));
		verify(trade, times(2)).applyTrade("r", "u1", TradeCount.ZERO);
	}

	@Test
	void 본문의_userId는_무시하고_세션의_userId를_쓴다() {
		when(trade.applyTrade(any(), any(), any())).thenReturn(TradeCount.ZERO);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"r\",\"userId\":\"attacker\",\"buy\":1}"));

		verify(trade).applyTrade("r", "u1", new TradeCount(1, 0));
	}
}
