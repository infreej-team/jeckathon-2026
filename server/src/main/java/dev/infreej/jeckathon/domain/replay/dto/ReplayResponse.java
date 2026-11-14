package dev.infreej.jeckathon.domain.replay.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.room.entity.Room;

public record ReplayResponse(String inviteCode, String name, Instant endedAt, double replayScore) {

	public static ReplayResponse from(Room room) {
		return new ReplayResponse(room.getInviteCode(), room.getName(), room.getEndedAt(), room.getReplayScore());
	}
}
