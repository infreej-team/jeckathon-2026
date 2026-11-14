package dev.infreej.jeckathon.domain.match.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.match.entity.Match;
import dev.infreej.jeckathon.domain.match.repository.MatchRepository;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

	private final MatchRepository matchRepository;

	public Page<Match> getMatches(int page, int size) {
		return matchRepository.findAll(PageRequest.of(page, size, Sort.by("kickoffAt", "id")));
	}

	public Match getMatch(Long id) {
		return matchRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
	}
}
