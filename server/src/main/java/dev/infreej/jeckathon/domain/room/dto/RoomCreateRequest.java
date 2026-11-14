package dev.infreej.jeckathon.domain.room.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// name을 비우면 "팀1 vs 팀2"로 채운다.
public record RoomCreateRequest(@NotNull(message = "필수 값입니다.") Long matchId,
		@Size(max = 100, message = "100자 이하여야 합니다.") String name) {
}
