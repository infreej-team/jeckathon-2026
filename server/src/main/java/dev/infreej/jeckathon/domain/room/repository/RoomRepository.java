package dev.infreej.jeckathon.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

	@EntityGraph(attributePaths = "match")
	List<Room> findAllByMatchIdOrderByCreatedAtAscIdAsc(Long matchId);

	@EntityGraph(attributePaths = "match")
	Page<Room> findAllByStatusOrderByCreatedAtDesc(RoomStatus status, Pageable pageable);

	// 로비 목록: 진행 중, 대기, 종료 순
	@Query(value = """
			select r from Room r left join fetch r.match
			order by case
				when r.status = dev.infreej.jeckathon.domain.room.entity.RoomStatus.LIVE then 0
				when r.status = dev.infreej.jeckathon.domain.room.entity.RoomStatus.WAITING then 1
				else 2 end, r.createdAt desc
			""", countQuery = "select count(r) from Room r")
	Page<Room> findAllForLobby(Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.inviteCode = :inviteCode")
	Optional<Room> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

	List<Room> findAllByStatusAndEndedAtIsNull(RoomStatus status);

	Page<Room> findByStatusAndReplayScoreIsNotNullOrderByReplayScoreDescEndedAtDescIdDesc(
		RoomStatus status, Pageable pageable
	);
}
