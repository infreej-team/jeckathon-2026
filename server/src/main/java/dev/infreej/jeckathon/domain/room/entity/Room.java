package dev.infreej.jeckathon.domain.room.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

	public Room(String inviteCode, String name) {
		this.inviteCode = inviteCode;
		this.name = name;
		this.status = RoomStatus.WAITING;
	}
}
