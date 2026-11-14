package dev.infreej.jeckathon.domain.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.market.dto.MarketTickMessage;
import dev.infreej.jeckathon.domain.market.dto.MarketTickResponse;
import dev.infreej.jeckathon.domain.market.entity.MarketTick;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.market.service.MarketRoomSource.ActiveRoom;
import dev.infreej.jeckathon.domain.market.service.MarketTradeSource.TradeVolume;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
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
	private final RoomRepository roomRepository;
	private final RoomSessionRegistry roomSessions;

	private final Map<Long, PendingTick> pendingTicks = new HashMap<>();
	// 초대 코드는 바뀌지 않으므로 방마다 한 번만 조회한다.
	private final Map<Long, String> inviteCodes = new HashMap<>();

	public PageResponse<MarketTickResponse> getTicks(Long roomId, int page, int size) {
		return PageResponse.from(
			repository.findByRoomIdOrderByTickedAtDesc(roomId, PageRequest.of(page, size)),
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
		Optional<MarketTick> previous = repository.findFirstByRoomIdOrderByTickedAtDesc(roomId);
		TradeVolume volume = trades.drain(roomId);
		if (skipEmpty && volume.isEmpty()) {
			return;
		}

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
			Math.toIntExact(volume.buyCount()),
			Math.toIntExact(volume.sellCount()),
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
			String inviteCode = inviteCodes.computeIfAbsent(roomId,
				id -> roomRepository.findById(id).map(Room::getInviteCode).orElseThrow());
			roomSessions.broadcast(inviteCode, MarketTickMessage.of(inviteCode, response));
		} catch (RuntimeException failure) {
			log.warn("Could not broadcast market tick {} for room {}", response.tickId(), roomId, failure);
		}

		return true;
	}

	private record PendingTick(Instant tickedAt, double indexValue, int buyCount, int sellCount, int participantCount) {
	}
}
