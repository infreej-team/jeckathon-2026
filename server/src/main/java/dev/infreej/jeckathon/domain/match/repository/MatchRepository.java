package dev.infreej.jeckathon.domain.match.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.infreej.jeckathon.domain.match.entity.Match;

public interface MatchRepository extends JpaRepository<Match, Long> {

	boolean existsByCompetition(String competition);
}
