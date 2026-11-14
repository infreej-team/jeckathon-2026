package dev.infreej.jeckathon.domain.room.service;

import org.springframework.stereotype.Service;

import dev.infreej.jeckathon.domain.market.service.MarketService;
import dev.infreej.jeckathon.domain.result.service.RoomResultService;
import dev.infreej.jeckathon.domain.result.service.RoomResultService.Result;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomEndService {

	private final RoomService roomService;
	private final MarketService marketService;
	private final RoomResultService roomResultService;

	public Room end(String inviteCode) {
		Room room = roomService.beginEnd(inviteCode);
		if (room.getStatus() == RoomStatus.ENDED) {
			return room;
		}

		// 거래 접수 종료를 커밋한 뒤 마지막 틱을 저장한다. 실패하면 같은 종료 요청으로 재시도한다.
		marketService.finishRoom(room);
		Result result = roomResultService.calculate(room.getId());
		return roomService.completeEnd(inviteCode, result.changeRate(), result.volatility(), result.replayScore());
	}
}
