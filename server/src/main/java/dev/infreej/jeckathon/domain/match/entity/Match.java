package dev.infreej.jeckathon.domain.match.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String competition;

	// 예: Matchday 1, Round of 16, Final
	@Column(nullable = false, length = 50)
	private String round;

	// 조별리그에만 있다. 예: Group A
	@Column(length = 20)
	private String groupName;

	@Column(nullable = false)
	private Instant kickoffAt;

	@Column(nullable = false, length = 50)
	private String team1;

	@Column(nullable = false, length = 50)
	private String team2;

	@Column(nullable = false, length = 100)
	private String venue;

	// 연장전까지 포함한 최종 점수. 응답에는 스포일러가 되므로 내보내지 않는다.
	private Integer score1;

	private Integer score2;

	private Integer penalty1;

	private Integer penalty2;

	public Match(String competition, String round, String groupName, Instant kickoffAt, String team1, String team2,
			String venue, Integer score1, Integer score2, Integer penalty1, Integer penalty2) {
		this.competition = competition;
		this.round = round;
		this.groupName = groupName;
		this.kickoffAt = kickoffAt;
		this.team1 = team1;
		this.team2 = team2;
		this.venue = venue;
		this.score1 = score1;
		this.score2 = score2;
		this.penalty1 = penalty1;
		this.penalty2 = penalty2;
	}
}
