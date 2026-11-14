package dev.infreej.jeckathon.domain.trade;

import java.util.Map;

public record DrainResult(TradeCount total, Map<String, TradeCount> byUser) {

	public static final DrainResult EMPTY = new DrainResult(TradeCount.ZERO, Map.of());

	public DrainResult {
		byUser = Map.copyOf(byUser);
	}
}
