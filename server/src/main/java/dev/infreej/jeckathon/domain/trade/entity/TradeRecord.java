package dev.infreej.jeckathon.domain.trade.entity;

import java.time.Instant;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 방 하나에서 사용자 한 명이 실제로 반영한 매수·매도 누적 횟수. 내 거래 기록과 명예의 전당이 읽는다.
@Entity
@Table(name = "trade_record",
	uniqueConstraints = @UniqueConstraint(name = "uk_trade_record_room_user", columnNames = {"room_id", "user_id"}),
	indexes = @Index(name = "idx_trade_record_user", columnList = "user_id, last_traded_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private Room room;

	@Column(nullable = false, length = 36)
	private String userId;

	@Column(nullable = false)
	private int buyCount;

	@Column(nullable = false)
	private int sellCount;

	@Column(nullable = false)
	private Instant lastTradedAt;
}
