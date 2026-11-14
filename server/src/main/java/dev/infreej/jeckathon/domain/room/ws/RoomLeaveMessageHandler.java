package dev.infreej.jeckathon.domain.room.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.global.websocket.WsMessageHandler;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

// { "type": "room.leave" }. 별도 응답 없이, 참여자 수가 바뀌면 남은 세션에 room.participants를 보낸다.
// 입장하지 않은 세션이면 아무것도 하지 않는다. 연결이 끊기면 RoomSessionRegistry가 자동으로 퇴장 처리한다.
@Component
@RequiredArgsConstructor
public class RoomLeaveMessageHandler implements WsMessageHandler {

	private final RoomSessionRegistry registry;

	@Override
	public String type() {
		return "room.leave";
	}

	@Override
	public void handle(WebSocketSession session, JsonNode payload) {
		registry.leave(session);
	}
}
