package dev.infreej.jeckathon.domain.market.service;

import java.util.List;

public interface MarketRoomSource {

	List<ActiveRoom> findActiveRooms();

	record ActiveRoom(Long roomId, int participantCount) {
	}
}
