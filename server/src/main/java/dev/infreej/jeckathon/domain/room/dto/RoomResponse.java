package dev.infreej.jeckathon.domain.room.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.match.dto.MatchResponse;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;

// 결산 값(changeRate, volatility, replayScore)은 경기가 끝나기 전에는 null이다.
// match는 경기 데이터를 넣기 전에 만든 방에서 null이다.
public record RoomResponse(String inviteCode, String name, MatchResponse match, RoomStatus status,
		Instant startedAt, Instant endedAt, Double changeRate, Double volatility, Double replayScore,
		Instant createdAt) {

	public static RoomResponse from(Room room) {
		MatchResponse match = room.getMatch() == null ? null : MatchResponse.from(room.getMatch());
		return new RoomResponse(room.getInviteCode(), room.getName(), match, room.getStatus(), room.getStartedAt(),
				room.getEndedAt(), room.getChangeRate(), room.getVolatility(), room.getReplayScore(),
				room.getCreatedAt());
	}
}
