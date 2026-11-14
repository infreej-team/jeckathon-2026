package dev.infreej.jeckathon.domain.room.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.infreej.jeckathon.domain.market.service.MarketService;
import dev.infreej.jeckathon.domain.trade.service.TradeService;
import lombok.RequiredArgsConstructor;

// 종료가 커밋된 뒤에 돈다. 그때부터 방이 LIVE가 아니므로 새 거래가 들어오지 않고, 1초 집계 대상에서도 빠진다.
// 남은 거래로 마지막 시세를 기록한 다음에 거래 카운터를 지운다.
@Component
@RequiredArgsConstructor
public class RoomEndedListener {

	private final MarketService marketService;
	private final TradeService tradeService;
	private final RoomSessionRegistry registry;

	@TransactionalEventListener
	public void onRoomEnded(RoomEndedEvent event) {
		try {
			marketService.finishRoom(event.roomId(), registry.participantCount(event.inviteCode()));
		} finally {
			tradeService.clearRoom(event.roomId());
		}
	}
}
