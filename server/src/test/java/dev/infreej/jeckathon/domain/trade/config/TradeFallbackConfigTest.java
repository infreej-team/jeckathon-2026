package dev.infreej.jeckathon.domain.trade.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import dev.infreej.jeckathon.domain.trade.InMemoryTradeService;
import dev.infreej.jeckathon.domain.trade.RoomQuery;
import dev.infreej.jeckathon.domain.trade.TradeService;
import dev.infreej.jeckathon.domain.trade.ws.NoDb;

class TradeFallbackConfigTest {

	@Nested
	@SpringBootTest(properties = NoDb.EXCLUDE)
	class 방_구현이_없으면 {
		@Autowired ApplicationContext context;

		@Test
		void 앱이_뜨고_임시_RoomQuery가_모두_허용한다() {
			RoomQuery rooms = context.getBean(RoomQuery.class);
			assertThat(context.getBeanNamesForType(RoomQuery.class)).containsExactly("allowAllRoomQuery");
			assertThat(rooms.exists("x") && rooms.isLive("x") && rooms.isMember("x", "u")).isTrue();
			assertThat(context.getBean(TradeService.class)).isInstanceOf(InMemoryTradeService.class);
		}
	}

	@Nested
	@SpringBootTest(properties = NoDb.EXCLUDE)
	@Import(RealRooms.class)
	class 방_구현이_있으면 {
		@Autowired ApplicationContext context;

		@Test
		void 임시_RoomQuery는_등록되지_않는다() {
			assertThat(context.getBeanNamesForType(RoomQuery.class)).containsExactly("realRoomQuery");
		}
	}

	@TestConfiguration
	static class RealRooms {
		@Bean
		RoomQuery realRoomQuery() {
			return new RoomQuery() {
				public boolean exists(String r) { return false; }
				public boolean isLive(String r) { return false; }
				public boolean isMember(String r, String u) { return false; }
			};
		}
	}
}
