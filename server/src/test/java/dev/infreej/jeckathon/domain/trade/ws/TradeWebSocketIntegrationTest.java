package dev.infreej.jeckathon.domain.trade.ws;

import static dev.infreej.jeckathon.global.websocket.FakeSessions.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.trade.TradeCount;
import dev.infreej.jeckathon.domain.trade.TradeService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = NoDb.EXCLUDE)
class TradeWebSocketIntegrationTest {
	@Value("${local.server.port}") int port;
	@MockitoBean TradeService tradeService;
	// DB 자동 설정을 뺐으므로 저장소 빈은 목으로 채운다.
	@MockitoBean RoomRepository roomRepository;
	@MockitoBean MarketTickRepository marketTickRepository;

	@Test
	void 접속해서_trade를_보내면_ack를_받는다() throws Exception {
		when(tradeService.applyTrade("room-1", "u1", new TradeCount(5, 0))).thenReturn(new TradeCount(3, 0));
		BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
		WebSocketSession ws = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
			protected void handleTextMessage(WebSocketSession s, TextMessage m) { inbox.add(m.getPayload()); }
		}, "ws://localhost:" + port + "/api/ws?userId=u1").get(5, TimeUnit.SECONDS);

		ws.sendMessage(new TextMessage("{\"type\":\"trade\",\"roomId\":\"room-1\",\"buy\":5,\"sell\":0}"));
		String ack = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("garbage"));
		String error = inbox.poll(5, TimeUnit.SECONDS);
		ws.sendMessage(new TextMessage("{\"type\":\"trade\",\"roomId\":\"room-1\",\"buy\":5,\"sell\":0}"));
		String second = inbox.poll(5, TimeUnit.SECONDS);

		assertThat(json(ack)).isEqualTo(json("{\"type\":\"trade.ack\",\"roomId\":\"room-1\",\"buy\":3,\"sell\":0}"));
		assertThat(json(error).get("type").asString()).isEqualTo("error");
		assertThat(second).as("error 후에도 연결이 유지된다").isNotNull();
		assertThat(ws.isOpen()).isTrue();
		verify(tradeService, org.mockito.Mockito.times(2)).applyTrade("room-1", "u1", new TradeCount(5, 0));
		ws.close();
	}
}
