package dev.infreej.jeckathon.domain.room.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.match.dto.MatchResponse;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;

// 결산 값(changeRate, volatility, replayScore)은 경기가 끝나기 전에는 null이다.
// match는 경기 데이터를 넣기 전에 만든 방에서 null이다.
// team은 방에 모이는 응원 팀이고, 두 팀 팬이 함께 쓰는 방에서는 null이다.
// participantCount는 지금 웹소켓으로 방에 들어와 있는 세션 수다.
public record RoomResponse(Long roomId, String inviteCode, String name, MatchResponse match, String team, int capacity,
		RoomStatus status, int participantCount, Instant startedAt, Instant endedAt, Double changeRate, Double volatility, Double replayScore,
		Instant createdAt) {

	public static RoomResponse from(Room room, int participantCount) {
		MatchResponse match = room.getMatch() == null ? null : MatchResponse.from(room.getMatch());
		return new RoomResponse(room.getId(), room.getInviteCode(), room.getName(), match, room.getTeam(), room.getCapacity(), room.getStatus(),
				participantCount, room.getStartedAt(), room.getEndedAt(), room.getChangeRate(), room.getVolatility(), room.getReplayScore(),
				room.getCreatedAt());
	}
}
