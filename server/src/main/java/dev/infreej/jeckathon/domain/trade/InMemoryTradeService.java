package dev.infreej.jeckathon.domain.trade;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class InMemoryTradeService implements TradeService {

    private final ConcurrentMap<String, RoomBucket> buckets = new ConcurrentHashMap<>();
    private final RoomQuery roomQuery;

    public InMemoryTradeService(RoomQuery roomQuery) {
        this.roomQuery = roomQuery;
    }

    @Override
    public TradeCount applyTrade(String roomId, String userId, TradeCount request) {
        if (request == null || roomId == null || userId == null) {
            return TradeCount.ZERO;
        }
        int buy = Math.max(request.buy(), 0);
        int sell = Math.max(request.sell(), 0);
        if (buy == 0 && sell == 0) {
            return TradeCount.ZERO;
        }
        if (!roomQuery.exists(roomId) || !roomQuery.isLive(roomId) || !roomQuery.isMember(roomId, userId)) {
            return TradeCount.ZERO;
        }
        while (true) {
            TradeCount applied = buckets.computeIfAbsent(roomId, id -> new RoomBucket()).add(userId, buy, sell);
            if (applied != null) {
                return applied;
            }
        }
    }

    @Override
    public DrainResult drain(String roomId) {
        RoomBucket bucket = roomId == null ? null : buckets.get(roomId);
        return bucket == null ? DrainResult.EMPTY : bucket.drain();
    }

    @Override
    public void clearRoom(String roomId) {
        if (roomId == null) {
            return;
        }
        RoomBucket bucket = buckets.remove(roomId);
        if (bucket != null) {
            bucket.close();
        }
    }
}
