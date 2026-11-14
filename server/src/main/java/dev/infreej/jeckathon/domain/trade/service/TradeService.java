package dev.infreej.jeckathon.domain.trade.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.trade.dto.TradeCount;

// 방별 매수·매도 횟수를 1초 구간 단위로 모은다. 방 존재·참여자 확인은 방 도메인이 생기면 applyTrade에 다시 넣는다.
@Service
public class TradeService {

	private final ConcurrentMap<String, RoomBucket> buckets = new ConcurrentHashMap<>();

	/** 한도를 적용해 실제로 반영한 횟수를 반환한다. 반영하지 않으면 {@link TradeCount#ZERO}. */
	public TradeCount applyTrade(String roomId, String userId, TradeCount request) {
		if (request == null || roomId == null || userId == null) {
			return TradeCount.ZERO;
		}
		int buy = Math.max(request.buy(), 0);
		int sell = Math.max(request.sell(), 0);
		if (buy == 0 && sell == 0) {
			return TradeCount.ZERO;
		}
		while (true) {
			TradeCount applied = buckets.computeIfAbsent(roomId, id -> new RoomBucket()).add(userId, buy, sell);
			if (applied != null) {
				return applied;
			}
		}
	}

	/** 현재 집계 구간의 방 합계를 돌려주고 0으로 초기화한다. 거래가 없던 방은 {@link TradeCount#ZERO}. */
	public TradeCount drain(String roomId) {
		RoomBucket bucket = roomId == null ? null : buckets.get(roomId);
		return bucket == null ? TradeCount.ZERO : bucket.drain();
	}

	public void clearRoom(String roomId) {
		if (roomId == null) {
			return;
		}
		RoomBucket bucket = buckets.remove(roomId);
		if (bucket != null) {
			bucket.close();
		}
	}
}
