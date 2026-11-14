package dev.infreej.jeckathon.domain.market.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.market.entity.MarketTick;

public interface MarketTickRepository extends JpaRepository<MarketTick, Long> {

	Optional<MarketTick> findFirstByRoomIdOrderByTickIdDesc(Long roomId);

	Page<MarketTick> findByRoomIdOrderByTickIdDesc(Long roomId, Pageable pageable);

	@Modifying
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Query(value = """
		INSERT INTO market_tick (room_id, tick_id, recorded_at, index_value, buy_count, sell_count)
		VALUES (:roomId, :tickId, :recordedAt, :indexValue, :buyCount, :sellCount)
		ON CONFLICT (room_id, tick_id) DO NOTHING
		""", nativeQuery = true)
	void insertTick(
		@Param("roomId") Long roomId,
		@Param("tickId") long tickId,
		@Param("recordedAt") Instant recordedAt,
		@Param("indexValue") BigDecimal indexValue,
		@Param("buyCount") long buyCount,
		@Param("sellCount") long sellCount
	);
}
