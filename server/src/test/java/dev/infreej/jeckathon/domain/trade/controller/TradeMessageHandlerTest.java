package dev.infreej.jeckathon.domain.trade.controller;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.service.TradeService;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.websocket.WsSender;

class TradeMessageHandlerTest {
	TradeService trade = mock(TradeService.class);
	RoomSessionRegistry registry = mock(RoomSessionRegistry.class);
	TradeMessageHandler handler = new TradeMessageHandler(trade, registry, new WsSender(MAPPER));
	WebSocketSession session = session("s1", "u1");

	@BeforeEach
	void 방_ROOM1에_입장해_있다() {
		when(registry.isJoined(session, "ROOM1")).thenReturn(true);
	}

	@Test
	void 정상_메시지는_초대_코드와_세션의_userId로_반영하고_반환값을_ack로_보낸다() throws Exception {
		when(trade.applyTrade("ROOM1", "u1", new TradeCount(5, 1))).thenReturn(new TradeCount(3, 1));

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":5,\"sell\":1}"));

		assertThat(sent(session)).containsExactly(json("{\"type\":\"trade.ack\",\"roomId\":\"ROOM1\",\"buy\":3,\"sell\":1}"));
	}

	@Test
	void 입장하지_않은_방이면_반영하지_않고_0으로_ack한다() throws Exception {
		when(registry.isJoined(session, "ROOM1")).thenReturn(false);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":1}"));

		assertThat(sent(session)).containsExactly(json("{\"type\":\"trade.ack\",\"roomId\":\"ROOM1\",\"buy\":0,\"sell\":0}"));
		verifyNoInteractions(trade);
	}

	@Test
	void 없는_방이면_error로_응답한다() throws Exception {
		when(registry.isJoined(session, "GONE1")).thenReturn(true);
		when(trade.applyTrade(any(), any(), any())).thenThrow(new BusinessException(ErrorCode.ROOM_NOT_FOUND));

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"GONE1\",\"buy\":1}"));

		assertThat(sent(session)).extracting(n -> n.get("type").asString()).containsExactly("error");
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

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":\"2\",\"sell\":1.5}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":99999999999,\"sell\":-2}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\"}"));

		verify(trade).applyTrade("ROOM1", "u1", new TradeCount(0, -2));
		verify(trade, times(2)).applyTrade("ROOM1", "u1", TradeCount.ZERO);
	}

	@Test
	void 본문의_userId는_무시하고_세션의_userId를_쓴다() {
		when(trade.applyTrade(any(), any(), any())).thenReturn(TradeCount.ZERO);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"userId\":\"attacker\",\"buy\":1}"));

		verify(trade).applyTrade("ROOM1", "u1", new TradeCount(1, 0));
	}
}
