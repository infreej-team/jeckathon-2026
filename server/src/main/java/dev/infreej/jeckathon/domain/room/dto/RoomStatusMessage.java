package dev.infreej.jeckathon.domain.room.dto;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;

// 웹소켓: { "type": "room.status", "roomId": "<inviteCode>", "status": "LIVE" }
public record RoomStatusMessage(String type, String roomId, RoomStatus status) {

	public static RoomStatusMessage from(Room room) {
		return new RoomStatusMessage("room.status", room.getInviteCode(), room.getStatus());
	}
}
