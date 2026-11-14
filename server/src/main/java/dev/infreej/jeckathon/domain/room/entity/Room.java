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

	public static final int DEFAULT_CAPACITY = 2000;

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

	// 이 방에 모이는 응원 팀. 경기의 team1 또는 team2 값이다. null이면 두 팀 팬이 함께 쓰는 방이다.
	@Column(length = 50)
	private String team;

	// 정원. 이 컬럼이 생기기 전에 만든 방은 null이고 기본 정원을 쓴다.
	private Integer capacity;

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

	// 두 팀 팬이 함께 쓰는 기본 정원의 방
	public Room(String inviteCode, String name, Match match) {
		this(inviteCode, name, match, null, null);
	}

	public Room(String inviteCode, String name, Match match, String team, Integer capacity) {
		this.inviteCode = inviteCode;
		this.name = name;
		this.match = match;
		this.team = team;
		this.capacity = capacity;
		this.status = RoomStatus.WAITING;
	}

	public int getCapacity() {
		return capacity == null ? DEFAULT_CAPACITY : capacity;
	}

	public void start(Instant startedAt) {
		this.status = RoomStatus.LIVE;
		this.startedAt = startedAt;
	}

	// 종료 시각이 기록된 LIVE 방은 마지막 시세와 결산을 저장하는 중이다.
	public void beginEnd(Instant endedAt) {
		this.endedAt = endedAt;
	}

	public boolean isAcceptingTrades() {
		return status == RoomStatus.LIVE && endedAt == null;
	}

	public void completeEnd(double changeRate, double volatility, Double replayScore) {
		this.changeRate = changeRate;
		this.volatility = volatility;
		this.replayScore = replayScore;
		this.status = RoomStatus.ENDED;
	}
}
