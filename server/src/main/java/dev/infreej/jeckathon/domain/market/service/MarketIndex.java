package dev.infreej.jeckathon.domain.market.service;

import java.math.BigDecimal;
import java.math.MathContext;

import dev.infreej.jeckathon.domain.market.service.MarketTradeSource.TradeVolume;

final class MarketIndex {

	static final BigDecimal INITIAL_VALUE = new BigDecimal("100.00");

	private static final int MAX_CLICKS_PER_PARTICIPANT = 3;
	private static final BigDecimal MAX_CHANGE_RATE = new BigDecimal("0.02");
	private static final MathContext PRECISION = MathContext.DECIMAL64;

	private MarketIndex() {
	}

	static BigDecimal calculate(BigDecimal previous, TradeVolume volume, int participantCount) {
		BigDecimal netBuy = BigDecimal.valueOf(volume.buyCount()).subtract(BigDecimal.valueOf(volume.sellCount()));
		long maxClicks = (long) Math.max(participantCount, 1) * MAX_CLICKS_PER_PARTICIPANT;
		BigDecimal ratio = netBuy.divide(BigDecimal.valueOf(maxClicks), PRECISION)
			.max(BigDecimal.ONE.negate())
			.min(BigDecimal.ONE);
		return previous.multiply(BigDecimal.ONE.add(ratio.multiply(MAX_CHANGE_RATE)), PRECISION);
	}
}
