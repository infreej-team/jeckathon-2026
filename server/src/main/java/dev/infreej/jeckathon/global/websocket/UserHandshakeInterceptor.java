package dev.infreej.jeckathon.global.websocket;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

// 임시 규칙: 입장(join)이 생기기 전까지 접속 URL의 ?userId=... 를 사용자 식별자로 쓴다. 없으면 UUID를 만든다.
// 사용자 식별 방식을 바꿀 때는 이 클래스만 고친다. 처리기는 메시지 본문이 아니라 userIdOf로 userId를 얻는다.
public class UserHandshakeInterceptor implements HandshakeInterceptor {

	static final String USER_ID = "userId";

	public static String userIdOf(WebSocketSession session) {
		return (String) session.getAttributes().get(USER_ID);
	}

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Map<String, Object> attributes) {
		String userId = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst(USER_ID);
		if (userId != null) {
			userId = UriUtils.decode(userId, StandardCharsets.UTF_8).strip();
		}
		attributes.put(USER_ID, userId == null || userId.isEmpty() ? UUID.randomUUID().toString() : userId);
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}
}
