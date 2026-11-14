package dev.infreej.jeckathon.domain.trade.controller;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.service.TradeService;
import dev.infreej.jeckathon.global.websocket.WsSender;

class TradeMessageHandlerTest {
	TradeService trade = mock(TradeService.class);
	RoomRepository rooms = mock(RoomRepository.class);
	RoomSessionRegistry registry = mock(RoomSessionRegistry.class);
	TradeMessageHandler handler = new TradeMessageHandler(trade, rooms, registry, new WsSender(MAPPER));
	WebSocketSession session = session("s1", "u1");

	@BeforeEach
	void 진행_중인_방_ROOM1에_입장해_있다() {
		Room room = room(7L, "ROOM1");
		room.start(Instant.now());
		when(rooms.findByInviteCode("ROOM1")).thenReturn(Optional.of(room));
		when(registry.isJoined(session, "ROOM1")).thenReturn(true);
	}

	@Test
	void 정상_메시지는_방_id와_세션의_userId로_반영하고_반환값을_ack로_보낸다() throws Exception {
		when(trade.applyTrade(7L, "u1", new TradeCount(5, 1))).thenReturn(new TradeCount(3, 1));

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
	void 진행_중이_아니거나_없는_방이면_반영하지_않고_0으로_ack한다() throws Exception {
		when(rooms.findByInviteCode("WAIT1")).thenReturn(Optional.of(room(8L, "WAIT1")));
		when(registry.isJoined(session, "WAIT1")).thenReturn(true);
		when(registry.isJoined(session, "GONE1")).thenReturn(true);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"WAIT1\",\"buy\":1}"));
		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"GONE1\",\"buy\":1}"));

		assertThat(sent(session)).extracting(n -> n.get("buy").intValue()).containsExactly(0, 0);
		verifyNoInteractions(trade);
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

		verify(trade).applyTrade(7L, "u1", new TradeCount(0, -2));
		verify(trade, times(2)).applyTrade(7L, "u1", TradeCount.ZERO);
	}

	@Test
	void 본문의_userId는_무시하고_세션의_userId를_쓴다() {
		when(trade.applyTrade(any(), any(), any())).thenReturn(TradeCount.ZERO);

		handler.handle(session, json("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"userId\":\"attacker\",\"buy\":1}"));

		verify(trade).applyTrade(7L, "u1", new TradeCount(1, 0));
	}

	static Room room(Long id, String inviteCode) {
		Room room = new Room(inviteCode, "경기");
		ReflectionTestUtils.setField(room, "id", id);
		return room;
	}
}
