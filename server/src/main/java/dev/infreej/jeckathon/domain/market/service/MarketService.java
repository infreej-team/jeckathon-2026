package dev.infreej.jeckathon.domain.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.market.dto.MarketTickMessage;
import dev.infreej.jeckathon.domain.market.dto.MarketTickResponse;
import dev.infreej.jeckathon.domain.market.entity.MarketTick;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import dev.infreej.jeckathon.domain.trade.service.TradeService;
import dev.infreej.jeckathon.domain.trade.service.TradeService.TradeSnapshot;
import dev.infreej.jeckathon.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

	private final MarketTickRepository repository;
	private final RoomService roomService;
	private final TradeService tradeService;
	private final RoomSessionRegistry roomSessions;

	private final Map<Long, PendingTick> pendingTicks = new HashMap<>();

	public PageResponse<MarketTickResponse> getTicks(Long roomId, int page, int size) {
		return PageResponse.from(
			repository.findByRoomIdOrderByTickedAtDesc(roomId, PageRequest.of(page, size)),
			MarketTickResponse::from
		);
	}

	// 거래가 없던 1초는 지수가 변하지 않으므로 저장하지 않는다. 클라이언트가 빈 구간을 직전 값으로 채운다.
	@Scheduled(fixedRate = 1_000)
	synchronized void recordActiveRooms() {
		for (Room room : roomService.getLiveRooms()) {
			try {
				if (!roomService.getByInviteCode(room.getInviteCode()).isAcceptingTrades()) {
					continue;
				}
				if (!savePendingTick(room)) {
					recordTick(room);
				}
			} catch (RuntimeException failure) {
				log.error("Could not record market tick for room {}", room.getId(), failure);
			}
		}
	}

	// Call after the room is no longer active and trade intake has closed.
	public synchronized void finishRoom(Room room) {
		savePendingTick(room);
		recordTick(room);
		tradeService.clearRoom(room.getInviteCode());
	}

	private void recordTick(Room room) {
		Long roomId = room.getId();
		// Trade messages use the invite code as their room key.
		TradeSnapshot snapshot = tradeService.drainWithParticipants(room.getInviteCode());
		TradeCount volume = snapshot.count();
		if (TradeCount.ZERO.equals(volume)) {
			return;
		}
		int participantCount = snapshot.participantCount();
		Optional<MarketTick> previous = repository.findFirstByRoomIdOrderByTickedAtDesc(roomId);

		Instant tickedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		Instant previousTime = previous.map(MarketTick::getTickedAt).orElse(Instant.MIN);
		if (!tickedAt.isAfter(previousTime)) {
			tickedAt = previousTime.plusMillis(1).truncatedTo(ChronoUnit.MILLIS);
		}
		BigDecimal previousValue = previous.map(tick -> BigDecimal.valueOf(tick.getIndexValue()))
			.orElse(MarketIndex.INITIAL_VALUE);
		PendingTick tick = new PendingTick(
			tickedAt,
			MarketIndex.calculate(previousValue, volume, participantCount).doubleValue(),
			volume.buy(),
			volume.sell(),
			participantCount
		);

		pendingTicks.put(roomId, tick);
		savePendingTick(room);
	}

	private boolean savePendingTick(Room room) {
		Long roomId = room.getId();
		PendingTick tick = pendingTicks.get(roomId);
		if (tick == null) {
			return false;
		}

		repository.insertTick(
			roomId, tick.tickedAt(), tick.indexValue(), tick.buyCount(), tick.sellCount(), tick.participantCount()
		);
		MarketTickResponse response = repository.findByRoomIdAndTickedAt(roomId, tick.tickedAt())
			.map(MarketTickResponse::from)
			.orElseThrow();
		pendingTicks.remove(roomId);

		try {
			roomSessions.broadcast(room.getInviteCode(), MarketTickMessage.of(room.getInviteCode(), response));
		} catch (RuntimeException failure) {
			log.warn("Could not broadcast market tick {} for room {}", response.tickId(), roomId, failure);
		}

		return true;
	}

	private record PendingTick(Instant tickedAt, double indexValue, int buyCount, int sellCount, int participantCount) {
	}
}
