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
	private static final int MAX_USER_ID_LENGTH = 36;

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
		// REST의 X-User-Id와 같은 규칙(36자 이하)을 지켜야 거래 기록에 저장할 수 있다.
		boolean valid = userId != null && !userId.isEmpty() && userId.length() <= MAX_USER_ID_LENGTH;
		attributes.put(USER_ID, valid ? userId : UUID.randomUUID().toString());
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
			WebSocketHandler wsHandler, Exception exception) {
	}
}
