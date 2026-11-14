package dev.infreej.jeckathon.domain.room.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;

public interface RoomRepository extends JpaRepository<Room, Long> {

	Optional<Room> findByInviteCode(String inviteCode);

	boolean existsByInviteCode(String inviteCode);

	List<Room> findAllByStatus(RoomStatus status);
}
