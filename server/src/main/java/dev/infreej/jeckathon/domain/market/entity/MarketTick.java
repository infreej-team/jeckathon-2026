package dev.infreej.jeckathon.domain.market.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "market_tick", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "tick_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MarketTick {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "room_id", nullable = false)
	private Long roomId;

	@Column(name = "tick_id", nullable = false)
	private long tickId;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	@Column(name = "index_value", nullable = false, columnDefinition = "numeric")
	private BigDecimal indexValue;

	@Column(name = "buy_count", nullable = false)
	private long buyCount;

	@Column(name = "sell_count", nullable = false)
	private long sellCount;
}
