package dev.infreej.jeckathon.domain.trade.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.service.TradeService;
import dev.infreej.jeckathon.global.websocket.UserHandshakeInterceptor;
import dev.infreej.jeckathon.global.websocket.WsMessageHandler;
import dev.infreej.jeckathon.global.websocket.WsSender;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

// { "type": "trade", "roomId": "<inviteCode>", "buy": 2, "sell": 0 } → { "type": "trade.ack", "roomId": "<inviteCode>", "buy": 2, "sell": 0 }
// 입장한 방이 진행 중일 때만 반영한다. 반영하지 않으면 ack의 buy·sell이 0이다.
// 음수 처리와 구간당 한도는 TradeService가 맡는다. 여기서는 형식만 맞춘다.
@Component
@RequiredArgsConstructor
public class TradeMessageHandler implements WsMessageHandler {

	private final TradeService tradeService;
	private final RoomRepository roomRepository;
	private final RoomSessionRegistry roomSessions;
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
		String inviteCode = roomIdNode.asString();
		TradeCount request = new TradeCount(countOf(payload, "buy"), countOf(payload, "sell"));
		TradeCount applied = TradeCount.ZERO;
		if (roomSessions.isJoined(session, inviteCode)) {
			applied = roomRepository.findByInviteCode(inviteCode)
				.filter(room -> room.getStatus() == RoomStatus.LIVE)
				.map(Room::getId)
				.map(roomId -> tradeService.applyTrade(roomId, UserHandshakeInterceptor.userIdOf(session), request))
				.orElse(TradeCount.ZERO);
		}
		sender.send(session, new TradeAck("trade.ack", inviteCode, applied.buy(), applied.sell()));
	}

	// 정수가 아니거나(문자열, 소수, int 범위 밖) 없으면 0으로 본다.
	private static int countOf(JsonNode payload, String field) {
		JsonNode node = payload.get(field);
		return node != null && node.isInt() ? node.intValue() : 0;
	}

	record TradeAck(String type, String roomId, int buy, int sell) {
	}
}
