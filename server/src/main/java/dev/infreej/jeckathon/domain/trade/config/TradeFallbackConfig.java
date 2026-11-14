package dev.infreej.jeckathon.domain.trade.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.infreej.jeckathon.domain.trade.RoomQuery;

// 방 도메인이 RoomQuery 빈을 올리기 전까지 모든 방과 사용자의 거래를 허용한다. 실제 빈이 생기면 등록되지 않는다.
@Configuration
public class TradeFallbackConfig {

	@Bean
	@ConditionalOnMissingBean(RoomQuery.class)
	public RoomQuery allowAllRoomQuery() {
		return new RoomQuery() {
			@Override
			public boolean exists(String roomId) {
				return true;
			}

			@Override
			public boolean isLive(String roomId) {
				return true;
			}

			@Override
			public boolean isMember(String roomId, String userId) {
				return true;
			}
		};
	}
}
