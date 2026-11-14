package dev.infreej.jeckathon.domain.market.repository;

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

	Optional<MarketTick> findFirstByRoomIdOrderByTickedAtDesc(Long roomId);

	Page<MarketTick> findByRoomIdOrderByTickedAtDesc(Long roomId, Pageable pageable);

	Optional<MarketTick> findByRoomIdAndTickedAt(Long roomId, Instant tickedAt);

	@Query("""
		select min(t.indexValue) as lowestValue, max(t.indexValue) as highestValue,
		       sum(t.buyCount) as totalBuys, sum(t.sellCount) as totalSells
		from MarketTick t where t.room.id = :roomId
		""")
	TickTotals summarizeByRoomId(@Param("roomId") Long roomId);

	interface TickTotals {
		Double getLowestValue();
		Double getHighestValue();
		Long getTotalBuys();
		Long getTotalSells();
	}

	@Modifying
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Query(value = """
		INSERT INTO market_tick (room_id, ticked_at, index_value, buy_count, sell_count, participant_count)
		VALUES (:roomId, :tickedAt, :indexValue, :buyCount, :sellCount, :participantCount)
		ON CONFLICT (room_id, ticked_at) DO NOTHING
		""", nativeQuery = true)
	void insertTick(
		@Param("roomId") Long roomId,
		@Param("tickedAt") Instant tickedAt,
		@Param("indexValue") double indexValue,
		@Param("buyCount") int buyCount,
		@Param("sellCount") int sellCount,
		@Param("participantCount") int participantCount
	);
}
