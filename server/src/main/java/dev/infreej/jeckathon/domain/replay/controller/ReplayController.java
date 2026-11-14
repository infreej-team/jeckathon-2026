package dev.infreej.jeckathon.domain.replay.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.replay.dto.ReplayResponse;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/replays")
@RequiredArgsConstructor
public class ReplayController {

	private final RoomRepository roomRepository;

	@GetMapping
	public PageResponse<ReplayResponse> list(
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		return PageResponse.from(
			roomRepository.findByStatusAndReplayScoreIsNotNullOrderByReplayScoreDescEndedAtDescIdDesc(
				RoomStatus.ENDED, PageRequest.of(page, size)
			),
			ReplayResponse::from
		);
	}
}
