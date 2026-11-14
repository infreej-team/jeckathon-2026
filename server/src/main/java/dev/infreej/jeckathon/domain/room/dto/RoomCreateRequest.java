package dev.infreej.jeckathon.domain.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
		@NotBlank(message = "필수 값입니다.") @Size(max = 100, message = "100자 이하여야 합니다.") String name) {
}
