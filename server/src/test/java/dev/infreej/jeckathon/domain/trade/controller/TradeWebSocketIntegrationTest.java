package dev.infreej.jeckathon.domain.trade.controller;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import dev.infreej.jeckathon.domain.comment.repository.CommentLikeRepository;
import dev.infreej.jeckathon.domain.comment.repository.CommentRepository;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.service.TradeService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = NoDb.EXCLUDE)
class TradeWebSocketIntegrationTest {
	@Value("${local.server.port}") int port;
	@Autowired TradeService tradeService;
	// DB 자동 설정을 뺐으므로 저장소 빈은 목으로 채운다. findAllByStatus가 빈 목록이라 1초 집계는 돌지 않는다.
	@MockitoBean RoomRepository roomRepository;
	@MockitoBean MarketTickRepository marketTickRepository;
	@MockitoBean CommentRepository commentRepository;
	@MockitoBean CommentLikeRepository commentLikeRepository;

	@Test
	void 방에_입장한_뒤에만_trade가_반영되고_한도를_넘으면_ack가_줄어든다() throws Exception {
		Room room = TradeMessageHandlerTest.room(7L, "ROOM1");
		room.start(Instant.now());
		when(roomRepository.findByInviteCode("ROOM1")).thenReturn(Optional.of(room));
		BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
		WebSocketSession ws = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			protected void handleTextMessage(WebSocketSession s, TextMessage m) { inbox.add(m.getPayload()); }
		}, "ws://localhost:" + port + "/api/ws?userId=u1").get(5, TimeUnit.SECONDS);

		ws.sendMessage(new TextMessage("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":5,\"sell\":0}"));
		String beforeJoin = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("{\"type\":\"room.join\",\"roomId\":\"ROOM1\"}"));
		String joined = inbox.poll(5, TimeUnit.SECONDS);
		String participants = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":5,\"sell\":0}"));
		String ack = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("garbage"));
		String error = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("{\"type\":\"trade\",\"roomId\":\"ROOM1\",\"buy\":1,\"sell\":0}"));
		String second = inbox.poll(5, TimeUnit.SECONDS);

		assertThat(json(beforeJoin)).isEqualTo(json("{\"type\":\"trade.ack\",\"roomId\":\"ROOM1\",\"buy\":0,\"sell\":0}"));
		assertThat(json(joined).get("type").asString()).isEqualTo("room.joined");
		assertThat(json(participants).get("participantCount").intValue()).isEqualTo(1);
		assertThat(json(ack)).isEqualTo(json("{\"type\":\"trade.ack\",\"roomId\":\"ROOM1\",\"buy\":3,\"sell\":0}"));
		assertThat(json(error).get("type").asString()).isEqualTo("error");
		assertThat(json(second)).as("error 후에도 연결이 유지되고, 구간 한도 3을 다 써서 0이 반영된다")
			.isEqualTo(json("{\"type\":\"trade.ack\",\"roomId\":\"ROOM1\",\"buy\":0,\"sell\":0}"));
		assertThat(tradeService.drain(7L)).isEqualTo(new TradeCount(3, 0));
		ws.close();
	}
}
