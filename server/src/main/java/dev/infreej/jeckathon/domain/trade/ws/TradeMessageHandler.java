package dev.infreej.jeckathon.domain.trade.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.trade.TradeCount;
import dev.infreej.jeckathon.domain.trade.TradeService;
import dev.infreej.jeckathon.global.websocket.UserHandshakeInterceptor;
import dev.infreej.jeckathon.global.websocket.WsMessageHandler;
import dev.infreej.jeckathon.global.websocket.WsSender;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

// { "type": "trade", "roomId": "...", "buy": 2, "sell": 0 } → { "type": "trade.ack", "roomId": "...", "buy": 2, "sell": 0 }
// 음수 처리와 구간당 한도는 TradeService가 맡는다. 여기서는 형식만 맞춘다.
@Component
@RequiredArgsConstructor
public class TradeMessageHandler implements WsMessageHandler {

	private final TradeService tradeService;
	private final WsSender sender;

	@Override
	public String type() {
		return "trade";
	}

	@Override
	public void handle(WebSocketSession session, JsonNode payload) {
		JsonNode roomIdNode = payload.get("roomId");
		if (roomIdNode == null || !roomIdNode.isString() || roomIdNode.asString().isBlank()) {
			sender.sendError(session, "roomId가 필요합니다.");
			return;
		}
		String roomId = roomIdNode.asString();
		TradeCount request = new TradeCount(countOf(payload, "buy"), countOf(payload, "sell"));
		TradeCount applied = tradeService.applyTrade(roomId, UserHandshakeInterceptor.userIdOf(session), request);
		sender.send(session, new TradeAck("trade.ack", roomId, applied.buy(), applied.sell()));
	}

	// 정수가 아니거나(문자열, 소수, int 범위 밖) 없으면 0으로 본다.
	private static int countOf(JsonNode payload, String field) {
		JsonNode node = payload.get(field);
		return node != null && node.isInt() ? node.intValue() : 0;
	}

	record TradeAck(String type, String roomId, int buy, int sell) {
	}
}
