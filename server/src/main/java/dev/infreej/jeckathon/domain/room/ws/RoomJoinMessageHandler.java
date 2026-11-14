package dev.infreej.jeckathon.domain.room.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.websocket.WsMessageHandler;
import dev.infreej.jeckathon.global.websocket.WsSender;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

// { "type": "room.join", "roomId": "<inviteCode>" } → { "type": "room.joined", "roomId", "name", "status", "participantCount" }
// 방 상태와 무관하게 입장할 수 있지만 정원이 찬 방에는 들어갈 수 없다. 참여자 수가 바뀌면 방 전체에 room.participants를 보낸다.
@Component
@RequiredArgsConstructor
public class RoomJoinMessageHandler implements WsMessageHandler {

	private final RoomService roomService;
	private final RoomSessionRegistry registry;
	private final WsSender sender;

	@Override
	public String type() {
		return "room.join";
	}

	@Override
	public void handle(WebSocketSession session, JsonNode payload) {
		JsonNode roomIdNode = payload.get("roomId");
		if (roomIdNode == null || !roomIdNode.isString() || roomIdNode.asString().isBlank()) {
			sender.sendError(session, "roomId가 필요합니다.");
			return;
		}
		Room room;
		try {
			room = roomService.getByInviteCode(roomIdNode.asString());
		} catch (BusinessException e) {
			sender.sendError(session, e.getMessage());
			return;
		}
		String inviteCode = room.getInviteCode();
		// 정원 검사와 입장 사이에 다른 세션이 들어오면 정원을 조금 넘을 수 있다.
		if (!registry.isJoined(session, inviteCode) && registry.participantCount(inviteCode) >= room.getCapacity()) {
			sender.sendError(session, "정원이 찬 방입니다.");
			return;
		}
		boolean changed = registry.join(session, inviteCode);
		sender.send(session, new RoomJoined("room.joined", inviteCode, room.getName(), room.getStatus(),
				registry.participantCount(inviteCode)));
		if (changed) {
			registry.broadcastParticipants(inviteCode);
		}
	}

	record RoomJoined(String type, String roomId, String name, RoomStatus status, int participantCount) {
	}
}
