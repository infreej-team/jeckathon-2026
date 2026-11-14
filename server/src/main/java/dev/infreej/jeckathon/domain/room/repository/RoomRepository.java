package dev.infreej.jeckathon.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Room r where r.inviteCode = :inviteCode")
	Optional<Room> findByInviteCodeForUpdate(@Param("inviteCode") String inviteCode);

	List<Room> findAllByStatusAndEndedAtIsNull(RoomStatus status);

	Page<Room> findByStatusAndReplayScoreIsNotNullOrderByReplayScoreDescEndedAtDescIdDesc(
		RoomStatus status, Pageable pageable
	);
}
