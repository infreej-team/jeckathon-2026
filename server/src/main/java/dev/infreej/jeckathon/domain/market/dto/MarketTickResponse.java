package dev.infreej.jeckathon.domain.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

import dev.infreej.jeckathon.domain.market.entity.MarketTick;

public record MarketTickResponse(
	long tickId,
	Instant recordedAt,
	BigDecimal indexValue,
	long buyCount,
	long sellCount
) {

	public static MarketTickResponse from(MarketTick tick) {
		return new MarketTickResponse(
			tick.getTickId(),
			tick.getRecordedAt(),
			tick.getIndexValue(),
			tick.getBuyCount(),
			tick.getSellCount()
		);
	}
}
