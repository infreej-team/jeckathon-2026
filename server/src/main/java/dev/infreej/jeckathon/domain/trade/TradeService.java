package dev.infreej.jeckathon.domain.trade;

public interface TradeService {

    /** 한도를 적용해 실제로 반영한 횟수를 반환한다. 반영하지 않으면 {@link TradeCount#ZERO}. */
    TradeCount applyTrade(String roomId, String userId, TradeCount request);

    /** 현재 집계 구간의 방 합계와 사용자별 값을 돌려주고 0으로 초기화한다. */
    DrainResult drain(String roomId);

    void clearRoom(String roomId);
}
