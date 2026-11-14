package dev.infreej.jeckathon.domain.comment.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.infreej.jeckathon.domain.comment.entity.Comment;
import dev.infreej.jeckathon.domain.room.entity.Room;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	Page<Comment> findByRoom(Room room, Pageable pageable);

	Page<Comment> findByRoomAndCreatedAtAfter(Room room, Instant since, Pageable pageable);

	// 동시에 들어온 좋아요가 서로 덮어쓰지 않도록 DB에서 더한다.
	@Modifying
	@Query("update Comment c set c.likeCount = c.likeCount + :delta where c.id = :id")
	void addLikeCount(Long id, int delta);

	@Query("select c.likeCount from Comment c where c.id = :id")
	int findLikeCount(Long id);
}
