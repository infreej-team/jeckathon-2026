package dev.infreej.jeckathon.domain.room.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;

// 결산 값(changeRate, volatility, replayScore)은 경기가 끝나기 전에는 null이다.
public record RoomResponse(String inviteCode, String name, RoomStatus status, Instant startedAt, Instant endedAt,
		Double changeRate, Double volatility, Double replayScore, Instant createdAt) {

	public static RoomResponse from(Room room) {
		return new RoomResponse(room.getInviteCode(), room.getName(), room.getStatus(), room.getStartedAt(),
				room.getEndedAt(), room.getChangeRate(), room.getVolatility(), room.getReplayScore(),
				room.getCreatedAt());
	}
}
