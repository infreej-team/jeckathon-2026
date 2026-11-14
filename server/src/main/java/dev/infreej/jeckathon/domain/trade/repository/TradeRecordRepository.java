package dev.infreej.jeckathon.domain.trade.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.infreej.jeckathon.domain.trade.entity.TradeRecord;

public interface TradeRecordRepository extends JpaRepository<TradeRecord, Long> {

	@Modifying
	@Query(value = """
		INSERT INTO trade_record (room_id, user_id, buy_count, sell_count, last_traded_at)
		VALUES (:roomId, :userId, :buy, :sell, :tradedAt)
		ON CONFLICT (room_id, user_id) DO UPDATE SET
			buy_count = trade_record.buy_count + EXCLUDED.buy_count,
			sell_count = trade_record.sell_count + EXCLUDED.sell_count,
			last_traded_at = EXCLUDED.last_traded_at
		""", nativeQuery = true)
	void addTrades(@Param("roomId") Long roomId, @Param("userId") String userId, @Param("buy") int buy,
		@Param("sell") int sell, @Param("tradedAt") Instant tradedAt);

	@Query(value = "select t from TradeRecord t join fetch t.room r left join fetch r.match where t.userId = :userId order by t.lastTradedAt desc, t.id desc",
		countQuery = "select count(t) from TradeRecord t where t.userId = :userId")
	Page<TradeRecord> findByUserId(@Param("userId") String userId, Pageable pageable);

	// 끝난 방에서 지수가 오른 방은 매수 - 매도, 내린 방은 매도 - 매수를 점수로 더한다. 매수·매도를 같이 연타하면 0점이다.
	@Query(value = """
		SELECT t.user_id AS userId,
		       SUM(CASE WHEN r.change_rate > 0 THEN t.buy_count - t.sell_count
		                WHEN r.change_rate < 0 THEN t.sell_count - t.buy_count
		                ELSE 0 END) AS score,
		       SUM(t.buy_count + t.sell_count) AS tradeCount,
		       COUNT(*) AS roomCount,
		       (SELECT c.nickname FROM comment c WHERE c.user_id = t.user_id ORDER BY c.id DESC LIMIT 1) AS nickname
		FROM trade_record t JOIN room r ON r.id = t.room_id
		WHERE r.status = 'ENDED' AND r.change_rate IS NOT NULL
		GROUP BY t.user_id
		ORDER BY score DESC, tradeCount DESC, t.user_id
		""",
		countQuery = """
		SELECT COUNT(DISTINCT t.user_id) FROM trade_record t JOIN room r ON r.id = t.room_id
		WHERE r.status = 'ENDED' AND r.change_rate IS NOT NULL
		""",
		nativeQuery = true)
	Page<HallOfFameRow> findHallOfFame(Pageable pageable);

	interface HallOfFameRow {
		String getUserId();
		Long getScore();
		Long getTradeCount();
		Long getRoomCount();
		String getNickname();
	}
}
