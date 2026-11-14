package dev.infreej.jeckathon.domain.market.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.market.entity.MarketTick;

public record MarketTickResponse(
	long tickId,
	Instant recordedAt,
	double indexValue,
	long buyCount,
	long sellCount
) {

	public static MarketTickResponse from(MarketTick tick) {
		return new MarketTickResponse(
			tick.getId(),
			tick.getTickedAt(),
			tick.getIndexValue(),
			tick.getBuyCount(),
			tick.getSellCount()
		);
	}
}
