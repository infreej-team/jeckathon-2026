package dev.infreej.jeckathon.domain.trade.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.trade.entity.TradeRecord;

// score는 방이 끝나 등락률이 정해진 뒤에만 값이 있다.
public record MyTradeResponse(String inviteCode, String roomName, String team, Long matchId, RoomStatus status,
		int buyCount, int sellCount, Double changeRate, Integer score, Instant lastTradedAt) {

	public static MyTradeResponse from(TradeRecord record) {
		Room room = record.getRoom();
		Double changeRate = room.getStatus() == RoomStatus.ENDED ? room.getChangeRate() : null;
		Integer score = changeRate == null ? null
			: changeRate > 0 ? record.getBuyCount() - record.getSellCount()
			: changeRate < 0 ? record.getSellCount() - record.getBuyCount()
			: 0;
		return new MyTradeResponse(room.getInviteCode(), room.getName(), room.getTeam(), room.getMatch() == null ? null : room.getMatch().getId(),
			room.getStatus(), record.getBuyCount(), record.getSellCount(), changeRate, score, record.getLastTradedAt());
	}
}
