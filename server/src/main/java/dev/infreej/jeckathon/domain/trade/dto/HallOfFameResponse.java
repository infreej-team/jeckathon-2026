package dev.infreej.jeckathon.domain.trade.dto;

import dev.infreej.jeckathon.domain.trade.repository.TradeRecordRepository.HallOfFameRow;

// userId는 X-User-Id로 쓰이는 사실상의 인증값이라 내보내지 않는다. 본인 행만 me = true로 알린다.
public record HallOfFameResponse(int rank, String displayName, long score, long tradeCount, long roomCount,
		boolean me) {

	public static HallOfFameResponse of(int rank, HallOfFameRow row, String requesterId) {
		String userId = row.getUserId();
		String displayName = row.getNickname() != null ? row.getNickname()
			: "팬 " + userId.substring(Math.max(0, userId.length() - 4));
		return new HallOfFameResponse(rank, displayName, row.getScore(), row.getTradeCount(), row.getRoomCount(),
			userId.equals(requesterId));
	}
}
