package dev.infreej.jeckathon.domain.trade;

public record TradeCount(int buy, int sell) {

    public static final TradeCount ZERO = new TradeCount(0, 0);
}
