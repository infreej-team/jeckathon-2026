package dev.infreej.jeckathon.domain.trade.service;

import org.springframework.stereotype.Component;

import dev.infreej.jeckathon.domain.market.service.MarketTradeSource;
import dev.infreej.jeckathon.domain.trade.dto.TradeCount;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeMarketSource implements MarketTradeSource {

	private final TradeService tradeService;

	@Override
	public TradeVolume drain(Long roomId) {
		TradeCount count = tradeService.drain(roomId);
		return new TradeVolume(count.buy(), count.sell());
	}
}
