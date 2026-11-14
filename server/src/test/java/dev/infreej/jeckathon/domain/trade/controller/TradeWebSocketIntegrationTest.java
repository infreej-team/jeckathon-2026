package dev.infreej.jeckathon.domain.trade.controller;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.json;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import tools.jackson.databind.JsonNode;

// 실제 DB(로컬은 compose.yaml, CI는 postgres 서비스)에 붙여 전체 빈을 연결한 상태로 띄운다.
// 방 입장 → 거래 → 1초 시세 집계 → 웹소켓 브로드캐스트까지 한 번에 확인한다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TradeWebSocketIntegrationTest {
	@Value("${local.server.port}") int port;
	@Autowired RoomRepository roomRepository;
	@Autowired RoomService roomService;

	@Test
	void 방에_입장한_뒤에만_trade가_반영되고_시세로_이어진다() throws Exception {
		String code = UUID.randomUUID().toString().substring(0, 8);
		roomRepository.save(new Room(code, "통합 테스트", null));
		roomService.start(code);
		BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
		WebSocketSession ws = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			protected void handleTextMessage(WebSocketSession s, TextMessage m) { inbox.add(m.getPayload()); }
		}, "ws://localhost:" + port + "/api/ws?userId=u1").get(5, TimeUnit.SECONDS);

		ws.sendMessage(new TextMessage(trade(code, 5)));
		JsonNode beforeJoin = next(inbox, "trade.ack");
		ws.sendMessage(new TextMessage("{\"type\":\"room.join\",\"roomId\":\"" + code + "\"}"));
		next(inbox, "room.joined");
		JsonNode participants = next(inbox, "room.participants");
		ws.sendMessage(new TextMessage(trade(code, 5)));
		JsonNode ack = next(inbox, "trade.ack");
		ws.sendMessage(new TextMessage("garbage"));
		next(inbox, "error");
		ws.sendMessage(new TextMessage(trade(code, 1)));
		JsonNode second = next(inbox, "trade.ack");
		JsonNode tick = nextTickWithBuy(inbox);

		assertThat(beforeJoin.get("buy").intValue()).as("입장 전 거래는 반영하지 않는다").isZero();
		assertThat(participants.get("participantCount").intValue()).isEqualTo(1);
		assertThat(ack.get("buy").intValue()).as("구간 한도 3까지만 반영한다").isEqualTo(3);
		assertThat(second.get("buy").intValue()).as("error 후에도 연결이 유지되고, 한도를 다 써서 0이 반영된다").isZero();
		assertThat(tick.get("roomId").asString()).isEqualTo(code);
		assertThat(tick.get("buyCount").intValue()).isEqualTo(3);
		assertThat(tick.get("indexValue").doubleValue()).as("매수만 있었으므로 지수가 오른다").isGreaterThan(100.0);
		ws.close();
		roomService.end(code);
	}

	private static String trade(String code, int buy) {
		return "{\"type\":\"trade\",\"roomId\":\"" + code + "\",\"buy\":" + buy + ",\"sell\":0}";
	}

	// 1초마다 오는 market.tick이 응답 사이에 끼어들 수 있으므로 type으로 골라 받는다.
	private static JsonNode next(BlockingQueue<String> inbox, String type) throws InterruptedException {
		while (true) {
			String payload = inbox.poll(5, TimeUnit.SECONDS);
			assertThat(payload).as(type + " 메시지를 기다린다").isNotNull();
			JsonNode node = json(payload);
			if (type.equals(node.get("type").asString())) {
				return node;
			}
		}
	}

	private static JsonNode nextTickWithBuy(BlockingQueue<String> inbox) throws InterruptedException {
		while (true) {
			JsonNode tick = next(inbox, "market.tick");
			if (tick.get("buyCount").intValue() > 0) {
				return tick;
			}
		}
	}
}
