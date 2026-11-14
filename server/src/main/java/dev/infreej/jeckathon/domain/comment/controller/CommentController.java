package dev.infreej.jeckathon.domain.comment.controller;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.comment.dto.CommentCreateRequest;
import dev.infreej.jeckathon.domain.comment.dto.CommentLikeResponse;
import dev.infreej.jeckathon.domain.comment.dto.CommentResponse;
import dev.infreej.jeckathon.domain.comment.service.CommentService;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 사용자는 브라우저가 만든 익명 UUID(X-User-Id 헤더)로 구분한다.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

	private static final String USER_ID_HEADER = "X-User-Id";
	private static final int MAX_USER_ID_LENGTH = 36;

	private final CommentService commentService;

	@PostMapping("/rooms/{inviteCode}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	public CommentResponse create(@PathVariable String inviteCode,
			@RequestHeader(name = USER_ID_HEADER, required = false) String userId,
			@Valid @RequestBody CommentCreateRequest request) {
		return commentService.create(inviteCode, requireUserId(userId), request);
	}

	@GetMapping("/rooms/{inviteCode}/comments")
	public PageResponse<CommentResponse> list(@PathVariable String inviteCode,
			@RequestHeader(name = USER_ID_HEADER, required = false) String userId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
		return commentService.list(inviteCode, requireUserId(userId), since == null ? null : since.toInstant(), page,
				size);
	}

	@PostMapping("/comments/{commentId}/likes")
	public CommentLikeResponse like(@PathVariable Long commentId,
			@RequestHeader(name = USER_ID_HEADER, required = false) String userId) {
		return new CommentLikeResponse(commentService.like(commentId, requireUserId(userId)));
	}

	@DeleteMapping("/comments/{commentId}/likes")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unlike(@PathVariable Long commentId,
			@RequestHeader(name = USER_ID_HEADER, required = false) String userId) {
		commentService.unlike(commentId, requireUserId(userId));
	}

	private static String requireUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > MAX_USER_ID_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_USER_ID);
		}
		return userId;
	}
}
