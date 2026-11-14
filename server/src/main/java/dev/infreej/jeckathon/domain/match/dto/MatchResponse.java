package dev.infreej.jeckathon.domain.match.dto;

import java.time.Instant;

import dev.infreej.jeckathon.domain.match.entity.Match;

// groupName은 토너먼트 경기에서 null이다.
public record MatchResponse(Long id, String competition, String round, String groupName, Instant kickoffAt,
		String team1, String team2, String venue) {

	public static MatchResponse from(Match match) {
		return new MatchResponse(match.getId(), match.getCompetition(), match.getRound(), match.getGroupName(),
				match.getKickoffAt(), match.getTeam1(), match.getTeam2(), match.getVenue());
	}
}
