package dev.infreej.jeckathon.domain.market.entity;

import java.time.Instant;

import dev.infreej.jeckathon.domain.room.entity.Room;
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

// 방의 1초 단위 시세 기록
@Entity
@Table(name = "market_tick", uniqueConstraints = @UniqueConstraint(name = "uk_market_tick_room_ticked_at", columnNames = {"room_id", "ticked_at"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTick {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	// 집계 시각
	@Column(nullable = false)
	private Instant tickedAt;

	// 집계 후 지수
	@Column(nullable = false)
	private double indexValue;

	@Column(nullable = false)
	private int buyCount;

	@Column(nullable = false)
	private int sellCount;

	// 집계 시점 참여자 수
	@Column(nullable = false)
	private int participantCount;

	public MarketTick(Room room, Instant tickedAt, double indexValue, int buyCount, int sellCount,
			int participantCount) {
		this.room = room;
		this.tickedAt = tickedAt;
		this.indexValue = indexValue;
		this.buyCount = buyCount;
		this.sellCount = sellCount;
		this.participantCount = participantCount;
	}
}
