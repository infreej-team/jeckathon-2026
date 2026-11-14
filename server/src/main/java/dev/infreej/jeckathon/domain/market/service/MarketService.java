package dev.infreej.jeckathon.domain.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.market.dto.MarketTickResponse;
import dev.infreej.jeckathon.domain.market.entity.MarketTick;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.service.RoomService;
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
	private final SimpMessagingTemplate messaging;

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
				if (!roomService.getByInviteCode(room.getInviteCode()).isAcceptingTrades()) {
					continue;
				}
				if (!savePendingTick(room.getId())) {
					recordTick(room, false);
				}
			} catch (RuntimeException failure) {
				log.error("Could not record market tick for room {}", room.getId(), failure);
			}
		}
	}

	// Call after the room is no longer active and trade intake has closed.
	public synchronized void finishRoom(Room room) {
		savePendingTick(room.getId());
		recordTick(room, true);
		tradeService.clearRoom(room.getInviteCode());
	}

	private void recordTick(Room room, boolean skipEmpty) {
		Long roomId = room.getId();
		Optional<MarketTick> previous = repository.findFirstByRoomIdOrderByTickedAtDesc(roomId);
		// Trade messages use the invite code as their room key.
		TradeSnapshot snapshot = tradeService.drainWithParticipants(room.getInviteCode());
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
		savePendingTick(roomId);
	}

	private boolean savePendingTick(Long roomId) {
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
			messaging.convertAndSend("/topic/rooms/" + roomId + "/market", response);
		} catch (RuntimeException failure) {
			log.warn("Could not broadcast market tick {} for room {}", response.tickId(), roomId, failure);
		}

		return true;
	}

	private record PendingTick(Instant tickedAt, double indexValue, int buyCount, int sellCount, int participantCount) {
	}
}
