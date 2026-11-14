package dev.infreej.jeckathon.domain.comment.repository;

import java.util.Collection;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import dev.infreej.jeckathon.domain.comment.entity.CommentLike;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

	boolean existsByCommentIdAndUserId(Long commentId, String userId);

	@Modifying
	@Query("delete from CommentLike l where l.comment.id = :commentId and l.userId = :userId")
	int deleteByCommentIdAndUserId(Long commentId, String userId);

	@Query("select l.comment.id from CommentLike l where l.userId = :userId and l.comment.id in :commentIds")
	Set<Long> findLikedCommentIds(String userId, Collection<Long> commentIds);
}
