package dev.infreej.jeckathon.domain.trade;

import java.util.HashMap;
import java.util.Map;

/** 방 하나의 현재 집계 구간. 모든 접근은 이 객체의 모니터 락 안에서 일어난다. */
final class RoomBucket {

	static final int LIMIT_PER_USER = 3;

	private final Map<String, TradeCount> byUser = new HashMap<>();
	private int buy;
	private int sell;
	private boolean closed;

	/** 한도를 적용해 더한 만큼을 반환한다. clearRoom으로 닫힌 버킷이면 null을 반환해 호출자가 새 버킷으로 재시도하게 한다. */
	synchronized TradeCount add(String userId, int requestedBuy, int requestedSell) {
		if (closed) {
			return null;
		}
		TradeCount used = byUser.getOrDefault(userId, TradeCount.ZERO);
		int acceptedBuy = Math.min(requestedBuy, LIMIT_PER_USER - used.buy());
		int acceptedSell = Math.min(requestedSell, LIMIT_PER_USER - used.sell());
		if (acceptedBuy == 0 && acceptedSell == 0) {
			return TradeCount.ZERO;
		}
		byUser.put(userId, new TradeCount(used.buy() + acceptedBuy, used.sell() + acceptedSell));
		buy += acceptedBuy;
		sell += acceptedSell;
		return new TradeCount(acceptedBuy, acceptedSell);
	}

	synchronized TradeCount drain() {
		if (byUser.isEmpty()) {
			return TradeCount.ZERO;
		}
		TradeCount result = new TradeCount(buy, sell);
		byUser.clear();
		buy = 0;
		sell = 0;
		return result;
	}

	synchronized void close() {
		closed = true;
		byUser.clear();
		buy = 0;
		sell = 0;
	}
}
