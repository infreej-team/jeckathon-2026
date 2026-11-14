package dev.infreej.jeckathon.domain.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.market.dto.MarketTickResponse;
import dev.infreej.jeckathon.domain.market.entity.MarketTick;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.market.service.MarketRoomSource.ActiveRoom;
import dev.infreej.jeckathon.domain.market.service.MarketTradeSource.TradeVolume;
import dev.infreej.jeckathon.global.response.PageResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketService {

	private final MarketTickRepository repository;
	private final ObjectProvider<MarketRoomSource> roomSources;
	private final ObjectProvider<MarketTradeSource> tradeSources;
	private final SimpMessagingTemplate messaging;

	private final Map<Long, MarketTickResponse> pendingTicks = new HashMap<>();

	public PageResponse<MarketTickResponse> getTicks(Long roomId, int page, int size) {
		return PageResponse.from(
			repository.findByRoomIdOrderByTickIdDesc(roomId, PageRequest.of(page, size)),
			MarketTickResponse::from
		);
	}

	@Scheduled(fixedRate = 1_000)
	synchronized void recordActiveRooms() {
		MarketRoomSource rooms = roomSources.getIfAvailable();
		MarketTradeSource trades = tradeSources.getIfAvailable();
		if (rooms == null || trades == null) {
			return;
		}

		for (ActiveRoom room : rooms.findActiveRooms()) {
			try {
				if (!savePendingTick(room.roomId())) {
					recordTick(room.roomId(), room.participantCount(), trades, false);
				}
			} catch (RuntimeException failure) {
				log.error("Could not record market tick for room {}", room.roomId(), failure);
			}
		}
	}

	// Call after the room is no longer active and trade intake has closed.
	public synchronized void finishRoom(Long roomId, int participantCount) {
		MarketTradeSource trades = tradeSources.getObject();
		savePendingTick(roomId);
		recordTick(roomId, participantCount, trades, true);
	}

	private void recordTick(Long roomId, int participantCount, MarketTradeSource trades, boolean skipEmpty) {
		Optional<MarketTick> previous = repository.findFirstByRoomIdOrderByTickIdDesc(roomId);
		TradeVolume volume = trades.drain(roomId);
		if (skipEmpty && volume.isEmpty()) {
			return;
		}

		long nextTickId = previous.map(MarketTick::getTickId).orElse(0L) + 1;
		BigDecimal previousValue = previous.map(MarketTick::getIndexValue).orElse(MarketIndex.INITIAL_VALUE);
		MarketTickResponse tick = new MarketTickResponse(
			nextTickId,
			Instant.now().truncatedTo(ChronoUnit.MILLIS),
			MarketIndex.calculate(previousValue, volume, participantCount),
			volume.buyCount(),
			volume.sellCount()
		);

		pendingTicks.put(roomId, tick);
		savePendingTick(roomId);
	}

	private boolean savePendingTick(Long roomId) {
		MarketTickResponse tick = pendingTicks.get(roomId);
		if (tick == null) {
			return false;
		}

		repository.insertTick(
			roomId, tick.tickId(), tick.recordedAt(), tick.indexValue(), tick.buyCount(), tick.sellCount()
		);
		pendingTicks.remove(roomId);

		try {
			messaging.convertAndSend("/topic/rooms/" + roomId + "/market", tick);
		} catch (RuntimeException failure) {
			log.warn("Could not broadcast market tick {} for room {}", tick.tickId(), roomId, failure);
		}

		return true;
	}
}
