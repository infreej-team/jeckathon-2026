package dev.infreej.jeckathon.domain.room.service;

import java.util.List;

import org.springframework.stereotype.Component;

import dev.infreej.jeckathon.domain.market.service.MarketRoomSource;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;

// 시세 집계 대상은 진행 중인 방이고, 참여자 수는 지금 방에 입장해 있는 서로 다른 사용자 수다.
@Component
@RequiredArgsConstructor
public class RoomMarketSource implements MarketRoomSource {

	private final RoomRepository roomRepository;
	private final RoomSessionRegistry registry;

	@Override
	public List<ActiveRoom> findActiveRooms() {
		return roomRepository.findAllByStatus(RoomStatus.LIVE).stream()
			.map(room -> new ActiveRoom(room.getId(), registry.participantCount(room.getInviteCode())))
			.toList();
	}
}
