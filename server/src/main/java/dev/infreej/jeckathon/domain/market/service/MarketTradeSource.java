package dev.infreej.jeckathon.domain.market.service;

public interface MarketTradeSource {

	// Reads and clears both room counters atomically.
	TradeVolume drain(Long roomId);

	record TradeVolume(long buyCount, long sellCount) {

		boolean isEmpty() {
			return buyCount == 0 && sellCount == 0;
		}
	}
}
