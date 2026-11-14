package dev.infreej.jeckathon.domain.trade.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.trade.dto.HallOfFameResponse;
import dev.infreej.jeckathon.domain.trade.dto.MyTradeResponse;
import dev.infreej.jeckathon.domain.trade.repository.TradeRecordRepository;
import dev.infreej.jeckathon.domain.trade.repository.TradeRecordRepository.HallOfFameRow;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeRecordController {

	private static final String USER_ID_HEADER = "X-User-Id";
	private static final int MAX_USER_ID_LENGTH = 36;

	private final TradeRecordRepository repository;

	// 내 거래 기록: 거래한 방마다 한 줄, 최근 거래 순
	@GetMapping("/me/trades")
	public PageResponse<MyTradeResponse> myTrades(
		@RequestHeader(name = USER_ID_HEADER, required = false) String userId,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		return PageResponse.from(repository.findByUserId(requireUserId(userId), pageOf(page, size)),
			MyTradeResponse::from);
	}

	// 명예의 전당: 끝난 방에서 방향을 맞힌 거래 점수 순. X-User-Id를 보내면 본인 행에 me = true
	@GetMapping("/hall-of-fame")
	public PageResponse<HallOfFameResponse> hallOfFame(
		@RequestHeader(name = USER_ID_HEADER, required = false) String userId,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "20") int size
	) {
		Page<HallOfFameRow> rows = repository.findHallOfFame(pageOf(page, size));
		AtomicInteger rank = new AtomicInteger(page * size);
		List<HallOfFameResponse> items = rows.getContent().stream()
			.map(row -> HallOfFameResponse.of(rank.incrementAndGet(), row, userId))
			.toList();
		return new PageResponse<>(items, rows.getNumber(), rows.getSize(), rows.getTotalElements());
	}

	private static PageRequest pageOf(int page, int size) {
		if (page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return PageRequest.of(page, size);
	}

	private static String requireUserId(String userId) {
		if (userId == null || userId.isBlank() || userId.length() > MAX_USER_ID_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_USER_ID);
		}
		return userId;
	}
}
