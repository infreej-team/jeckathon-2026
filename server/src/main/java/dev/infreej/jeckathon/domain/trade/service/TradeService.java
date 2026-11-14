package dev.infreej.jeckathon.domain.trade.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.repository.TradeRecordRepository;
import lombok.RequiredArgsConstructor;

// 방별 매수·매도 횟수를 1초 구간 단위로 모은다.
@Service
@RequiredArgsConstructor
public class TradeService {

	private final RoomService roomService;
	private final TradeRecordRepository tradeRecordRepository;
	private final ConcurrentMap<String, RoomBucket> buckets = new ConcurrentHashMap<>();

	/** 한도를 적용해 실제로 반영한 횟수를 반환한다. 반영하지 않으면 {@link TradeCount#ZERO}. */
	@Transactional
	public TradeCount applyTrade(String roomId, String userId, TradeCount request) {
		if (request == null || roomId == null || userId == null) {
			return TradeCount.ZERO;
		}
		int buy = Math.max(request.buy(), 0);
		int sell = Math.max(request.sell(), 0);
		if (buy == 0 && sell == 0) {
			return TradeCount.ZERO;
		}
		// 종료 요청과 동일한 방 행을 잠근 상태에서 카운터에 반영한다.
		Room room = roomService.getByInviteCodeForUpdate(roomId);
		if (!room.isAcceptingTrades()) {
			return TradeCount.ZERO;
		}
		TradeCount applied;
		do {
			applied = buckets.computeIfAbsent(roomId, id -> new RoomBucket()).add(userId, buy, sell);
		} while (applied == null);
		// 내 거래 기록과 명예의 전당용 누적. 방 행 잠금 안이라 같은 방의 기록은 순서대로 쌓인다.
		if (!TradeCount.ZERO.equals(applied)) {
			tradeRecordRepository.addTrades(room.getId(), userId, applied.buy(), applied.sell(), Instant.now());
		}
		return applied;
	}

	/** 현재 집계 구간의 방 합계를 돌려주고 0으로 초기화한다. 거래가 없던 방은 {@link TradeCount#ZERO}. */
	public TradeCount drain(String roomId) {
		return drainWithParticipants(roomId).count();
	}

	public TradeSnapshot drainWithParticipants(String roomId) {
		RoomBucket bucket = roomId == null ? null : buckets.get(roomId);
		return bucket == null ? TradeSnapshot.EMPTY : bucket.drain();
	}

	public record TradeSnapshot(TradeCount count, int participantCount) {
		static final TradeSnapshot EMPTY = new TradeSnapshot(TradeCount.ZERO, 0);
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
