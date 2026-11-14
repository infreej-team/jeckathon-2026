package dev.infreej.jeckathon.domain.room.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import dev.infreej.jeckathon.domain.match.entity.Match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 초대 URL에 쓰는 임의 문자열
	@Column(nullable = false, unique = true, length = 12)
	private String inviteCode;

	// 경기 이름
	@Column(nullable = false, length = 100)
	private String name;

	// 경기 데이터를 넣기 전에 만든 방은 null이다.
	@ManyToOne
	@JoinColumn(name = "match_id")
	private Match match;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RoomStatus status;

	private Instant startedAt;

	private Instant endedAt;

	// 결산 값. 경기가 끝나면 채운다.
	private Double changeRate;

	private Double volatility;

	private Double replayScore;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	public Room(String inviteCode, String name, Match match) {
		this.inviteCode = inviteCode;
		this.name = name;
		this.match = match;
		this.status = RoomStatus.WAITING;
	}

	public void start(Instant startedAt) {
		this.status = RoomStatus.LIVE;
		this.startedAt = startedAt;
	}

	public void end(Instant endedAt) {
		this.status = RoomStatus.ENDED;
		this.endedAt = endedAt;
	}
}
