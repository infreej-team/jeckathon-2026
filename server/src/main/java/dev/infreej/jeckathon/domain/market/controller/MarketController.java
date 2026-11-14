package dev.infreej.jeckathon.domain.market.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.market.dto.MarketTickResponse;
import dev.infreej.jeckathon.domain.market.service.MarketService;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms/{roomId}/market/ticks")
@RequiredArgsConstructor
public class MarketController {

	private final MarketService marketService;

	@GetMapping
	public PageResponse<MarketTickResponse> getTicks(
		@PathVariable("roomId") Long roomId,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "120") int size
	) {
		if (roomId <= 0 || page < 0 || size < 1 || size > 600) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}

		return marketService.getTicks(roomId, page, size);
	}
}
