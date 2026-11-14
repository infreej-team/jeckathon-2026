package dev.infreej.jeckathon.domain.trade;

/** 방 도메인 포트. 구현은 방 도메인이 제공한다. */
public interface RoomQuery {

    boolean exists(String roomId);

    boolean isLive(String roomId);

    boolean isMember(String roomId, String userId);
}
