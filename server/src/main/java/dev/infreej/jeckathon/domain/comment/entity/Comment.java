package dev.infreej.jeckathon.domain.comment.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import dev.infreej.jeckathon.domain.room.entity.Room;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comment", indexes = @Index(name = "idx_comment_room_chart_time", columnList = "room_id, chart_time"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	// 브라우저가 만든 익명 UUID
	@Column(nullable = false, length = 36)
	private String userId;

	@Column(nullable = false, length = 20)
	private String nickname;

	@Column(nullable = false, length = 200)
	private String content;

	// 코멘트가 가리키는 차트 시점
	@Column(nullable = false)
	private Instant chartTime;

	@Column(nullable = false)
	private int likeCount;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public Comment(Room room, String userId, String nickname, String content, Instant chartTime) {
		this.room = room;
		this.userId = userId;
		this.nickname = nickname;
		this.content = content;
		this.chartTime = chartTime;
	}
}
