package dev.infreej.jeckathon.domain.room.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import dev.infreej.jeckathon.domain.room.dto.RoomParticipantsMessage;
import dev.infreej.jeckathon.global.websocket.UserHandshakeInterceptor;
import dev.infreej.jeckathon.global.websocket.WsSender;
import dev.infreej.jeckathon.global.websocket.WsSessionCloseListener;
import lombok.RequiredArgsConstructor;

// 방에 입장한 웹소켓 세션을 메모리로만 관리한다. 한 세션은 한 방에만 속한다.
// 웹소켓 처리 스레드와 시세 스케줄러가 함께 부르므로 상태는 lock 안에서만 바꾸고, 전송은 lock 밖에서 한다.
@Component
@RequiredArgsConstructor
public class RoomSessionRegistry implements WsSessionCloseListener {

	private final WsSender sender;
	private final Object lock = new Object();
	// sessionId → inviteCode
	private final Map<String, String> roomOfSession = new HashMap<>();
	// inviteCode → (sessionId → session)
	private final Map<String, Map<String, WebSocketSession>> sessionsByRoom = new HashMap<>();

	// 참여자 수는 서로 다른 userId 수다. 같은 사용자가 탭을 여러 개 열어도 1명이다.
	public int participantCount(String inviteCode) {
		synchronized (lock) {
			return countOf(inviteCode);
		}
	}

	public boolean isJoined(WebSocketSession session, String inviteCode) {
		synchronized (lock) {
			return Objects.equals(roomOfSession.get(session.getId()), inviteCode);
		}
	}

	public void broadcast(String inviteCode, Object message) {
		List<WebSocketSession> targets;
		synchronized (lock) {
			Map<String, WebSocketSession> sessions = sessionsByRoom.get(inviteCode);
			if (sessions == null) {
				return;
			}
			targets = List.copyOf(sessions.values());
		}
		// 그 사이 닫힌 세션은 WsSender가 건너뛴다.
		for (WebSocketSession session : targets) {
			sender.send(session, message);
		}
	}

	// 다른 방에 있던 세션이면 이전 방에서 먼저 뺀다. 입장한 방의 참여자 수가 바뀌었으면 true를 돌려준다.
	public boolean join(WebSocketSession session, String inviteCode) {
		String previous;
		boolean previousChanged = false;
		boolean changed;
		synchronized (lock) {
			// 연결 종료 처리가 먼저 끝난 세션을 다시 넣으면 참여자 수에서 빠지지 않는다.
			if (!session.isOpen()) {
				return false;
			}
			previous = roomOfSession.get(session.getId());
			if (inviteCode.equals(previous)) {
				return false;
			}
			if (previous != null) {
				previousChanged = remove(session, previous);
			}
			int before = countOf(inviteCode);
			sessionsByRoom.computeIfAbsent(inviteCode, k -> new HashMap<>()).put(session.getId(), session);
			roomOfSession.put(session.getId(), inviteCode);
			changed = countOf(inviteCode) != before;
		}
		if (previousChanged) {
			broadcastParticipants(previous);
		}
		return changed;
	}

	public void leave(WebSocketSession session) {
		String inviteCode;
		boolean changed;
		synchronized (lock) {
			inviteCode = roomOfSession.get(session.getId());
			if (inviteCode == null) {
				return;
			}
			changed = remove(session, inviteCode);
		}
		if (changed) {
			broadcastParticipants(inviteCode);
		}
	}

	public void broadcastParticipants(String inviteCode) {
		broadcast(inviteCode, new RoomParticipantsMessage("room.participants", inviteCode, participantCount(inviteCode)));
	}

	@Override
	public void onClose(WebSocketSession session) {
		leave(session);
	}

	// lock 안에서만 부른다. 참여자 수가 바뀌었으면 true를 돌려준다.
	private boolean remove(WebSocketSession session, String inviteCode) {
		int before = countOf(inviteCode);
		roomOfSession.remove(session.getId());
		Map<String, WebSocketSession> sessions = sessionsByRoom.get(inviteCode);
		if (sessions != null) {
			sessions.remove(session.getId());
			if (sessions.isEmpty()) {
				sessionsByRoom.remove(inviteCode);
			}
		}
		return countOf(inviteCode) != before;
	}

	// lock 안에서만 부른다.
	private int countOf(String inviteCode) {
		Map<String, WebSocketSession> sessions = sessionsByRoom.get(inviteCode);
		if (sessions == null) {
			return 0;
		}
		return (int) sessions.values().stream().map(UserHandshakeInterceptor::userIdOf).distinct().count();
	}
}
