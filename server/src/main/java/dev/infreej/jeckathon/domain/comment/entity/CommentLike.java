package dev.infreej.jeckathon.domain.comment.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 같은 사용자가 한 코멘트에 좋아요를 한 번만 누르게 한다.
@Entity
@Table(name = "comment_like", uniqueConstraints = @UniqueConstraint(name = "uk_comment_like_comment_user", columnNames = {
		"comment_id", "user_id" }))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "comment_id", nullable = false)
	private Comment comment;

	// 브라우저가 만든 익명 UUID
	@Column(nullable = false, length = 36)
	private String userId;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public CommentLike(Comment comment, String userId) {
		this.comment = comment;
		this.userId = userId;
	}
}
