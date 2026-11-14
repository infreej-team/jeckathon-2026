package dev.infreej.jeckathon.domain.result.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.market.entity.MarketTick;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository;
import dev.infreej.jeckathon.domain.market.repository.MarketTickRepository.TickTotals;
import dev.infreej.jeckathon.domain.market.service.MarketIndex;
import dev.infreej.jeckathon.domain.replay.service.ReplayScore;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomResultService {

	private final MarketTickRepository marketTickRepository;

	@Transactional(readOnly = true)
	public Result calculate(Long roomId) {
		TickTotals totals = marketTickRepository.summarizeByRoomId(roomId);
		if (totals.getTotalBuys() == null || (totals.getTotalBuys() == 0 && totals.getTotalSells() == 0)) {
			return new Result(0, 0, null);
		}

		double initialValue = MarketIndex.INITIAL_VALUE.doubleValue();
		double closingValue = marketTickRepository.findFirstByRoomIdOrderByTickedAtDesc(roomId)
			.map(MarketTick::getIndexValue).orElseThrow();
		double low = Math.min(initialValue, totals.getLowestValue());
		double high = Math.max(initialValue, totals.getHighestValue());

		double changeRate = (closingValue - initialValue) / initialValue * 100;
		double volatility = (high - low) / initialValue * 100;
		return new Result(changeRate, volatility, ReplayScore.calculate(changeRate, volatility));
	}

	public record Result(double changeRate, double volatility, Double replayScore) {
	}
}
