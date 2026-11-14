package dev.infreej.jeckathon.domain.match.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.match.dto.MatchResponse;
import dev.infreej.jeckathon.domain.match.service.MatchService;
import dev.infreej.jeckathon.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

	private final MatchService matchService;

	@GetMapping
	public PageResponse<MatchResponse> list(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return PageResponse.from(matchService.getMatches(Math.max(page, 0), Math.clamp(size, 1, 200)),
				MatchResponse::from);
	}

	@GetMapping("/{id}")
	public MatchResponse get(@PathVariable Long id) {
		return MatchResponse.from(matchService.getMatch(id));
	}
}
