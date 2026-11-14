package dev.infreej.jeckathon.domain.comment.service;

import java.time.Instant;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.comment.dto.CommentCreateRequest;
import dev.infreej.jeckathon.domain.comment.dto.CommentResponse;
import dev.infreej.jeckathon.domain.comment.entity.Comment;
import dev.infreej.jeckathon.domain.comment.entity.CommentLike;
import dev.infreej.jeckathon.domain.comment.repository.CommentLikeRepository;
import dev.infreej.jeckathon.domain.comment.repository.CommentRepository;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

	private static final int MAX_PAGE_SIZE = 100;
	private static final Sort CHART_ORDER = Sort.by("chartTime", "id");

	private final RoomService roomService;
	private final CommentRepository commentRepository;
	private final CommentLikeRepository commentLikeRepository;

	@Transactional
	public CommentResponse create(String inviteCode, String userId, CommentCreateRequest request) {
		Room room = roomService.getByInviteCode(inviteCode);
		if (room.getStatus() == RoomStatus.WAITING) {
			throw new BusinessException(ErrorCode.ROOM_NOT_STARTED);
		}
		Comment comment = commentRepository.saveAndFlush(
				new Comment(room, userId, request.nickname(), request.content(), request.chartTime()));
		return CommentResponse.of(comment, false);
	}

	// since가 있으면 그 뒤에 작성된 코멘트만 준다. 클라이언트가 몇 초마다 새 코멘트를 가져갈 때 쓴다.
	public PageResponse<CommentResponse> list(String inviteCode, String userId, Instant since, int page, int size) {
		Room room = roomService.getByInviteCode(inviteCode);
		PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), CHART_ORDER);
		Page<Comment> comments = since == null
			? commentRepository.findByRoom(room, pageable)
			: commentRepository.findByRoomAndCreatedAtAfter(room, since, pageable);
		Set<Long> liked = comments.isEmpty()
			? Set.of()
			: commentLikeRepository.findLikedCommentIds(userId, comments.map(Comment::getId).getContent());
		return PageResponse.from(comments, comment -> CommentResponse.of(comment, liked.contains(comment.getId())));
	}

	@Transactional
	public int like(Long commentId, String userId) {
		Comment comment = getComment(commentId);
		if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
			throw new BusinessException(ErrorCode.ALREADY_LIKED);
		}
		try {
			commentLikeRepository.saveAndFlush(new CommentLike(comment, userId));
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(ErrorCode.ALREADY_LIKED);
		}
		commentRepository.addLikeCount(commentId, 1);
		return commentRepository.findLikeCount(commentId);
	}

	@Transactional
	public void unlike(Long commentId, String userId) {
		getComment(commentId);
		if (commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId) == 0) {
			throw new BusinessException(ErrorCode.COMMENT_LIKE_NOT_FOUND);
		}
		commentRepository.addLikeCount(commentId, -1);
	}

	private Comment getComment(Long commentId) {
		return commentRepository.findById(commentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
	}
}
