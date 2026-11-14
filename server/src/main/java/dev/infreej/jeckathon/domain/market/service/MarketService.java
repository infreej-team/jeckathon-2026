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

	@Scheduled(fixedRate = 1_000)
	synchronized void recordActiveRooms() {
		for (Room room : roomService.getLiveRooms()) {
			try {
				if (!savePendingTick(room.getId(), room.getInviteCode())) {
					recordTick(room.getId(), room.getInviteCode(), false);
				}
			} catch (RuntimeException failure) {
				log.error("Could not record market tick for room {}", room.getId(), failure);
			}
		}
	}

	// Call after the room is no longer active and trade intake has closed.
	public synchronized void finishRoom(Long roomId, String inviteCode) {
		savePendingTick(roomId, inviteCode);
		recordTick(roomId, inviteCode, true);
	}

	private void recordTick(Long roomId, String inviteCode, boolean skipEmpty) {
		Optional<MarketTick> previous = repository.findFirstByRoomIdOrderByTickedAtDesc(roomId);
		TradeSnapshot snapshot = tradeService.drainWithParticipants(roomId);
		TradeCount volume = snapshot.count();
		if (skipEmpty && TradeCount.ZERO.equals(volume)) {
			return;
		}
		int participantCount = snapshot.participantCount();

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
		savePendingTick(roomId, inviteCode);
	}

	private boolean savePendingTick(Long roomId, String inviteCode) {
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
			roomSessions.broadcast(inviteCode, MarketTickMessage.of(inviteCode, response));
		} catch (RuntimeException failure) {
			log.warn("Could not broadcast market tick {} for room {}", response.tickId(), roomId, failure);
		}

		return true;
	}

	private record PendingTick(Instant tickedAt, double indexValue, int buyCount, int sellCount, int participantCount) {
	}
}
