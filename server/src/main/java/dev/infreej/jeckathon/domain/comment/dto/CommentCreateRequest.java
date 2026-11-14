package dev.infreej.jeckathon.domain.comment.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
		@NotBlank(message = "필수 값입니다.") @Size(max = 20, message = "20자 이하여야 합니다.") String nickname,
		@NotBlank(message = "필수 값입니다.") @Size(max = 200, message = "200자 이하여야 합니다.") String content,
		@NotNull(message = "필수 값입니다.") Instant chartTime) {
}
