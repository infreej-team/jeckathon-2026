package dev.infreej.jeckathon.domain.comment.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.comment.entity.Comment;

// 작성자의 userId는 내보내지 않는다.
public record CommentResponse(Long id, String nickname, String content, Instant chartTime, int likeCount,
		Instant createdAt, boolean likedByMe) {

	public static CommentResponse of(Comment comment, boolean likedByMe) {
		return new CommentResponse(comment.getId(), comment.getNickname(), comment.getContent(),
				comment.getChartTime(), comment.getLikeCount(), comment.getCreatedAt(), likedByMe);
	}
}
