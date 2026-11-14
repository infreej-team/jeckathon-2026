package dev.infreej.jeckathon.domain.market.dto;

import java.time.Instant;

// 웹소켓: { "type": "market.tick", "roomId": "<inviteCode>", "tickId", "recordedAt", "indexValue", "buyCount", "sellCount" }
public record MarketTickMessage(String type, String roomId, long tickId, Instant recordedAt, double indexValue,
		long buyCount, long sellCount) {

	public static MarketTickMessage of(String inviteCode, MarketTickResponse tick) {
		return new MarketTickMessage("market.tick", inviteCode, tick.tickId(), tick.recordedAt(), tick.indexValue(),
				tick.buyCount(), tick.sellCount());
	}
}
