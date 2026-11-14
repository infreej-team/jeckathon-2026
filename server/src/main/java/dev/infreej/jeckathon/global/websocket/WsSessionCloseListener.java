package dev.infreej.jeckathon.global.websocket;

import org.springframework.web.socket.WebSocketSession;

// 웹소켓 연결이 끊길 때 호출된다. 빈으로 등록하면 GatewayWebSocketHandler가 자동으로 찾는다.
public interface WsSessionCloseListener {

	void onClose(WebSocketSession session);
}
