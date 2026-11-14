package dev.infreej.jeckathon.domain.room.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// name을 비우면 "팀1 vs 팀2"로 채운다.
// team은 경기의 team1 또는 team2 값이다. 비우면 두 팀 팬이 함께 쓰는 방이 된다.
// capacity를 비우면 기본 정원(2000)을 쓴다.
public record RoomCreateRequest(@NotNull(message = "필수 값입니다.") Long matchId,
		@Size(max = 100, message = "100자 이하여야 합니다.") String name,
		@Size(max = 50, message = "50자 이하여야 합니다.") String team,
		@Min(value = 1, message = "1 이상이어야 합니다.") @Max(value = 100000, message = "100000 이하여야 합니다.") Integer capacity) {
}
