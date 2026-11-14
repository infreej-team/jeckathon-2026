package dev.infreej.jeckathon.domain.match.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import dev.infreej.jeckathon.domain.match.entity.Match;
import dev.infreej.jeckathon.domain.match.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

// openfootball/worldcup.json의 2026/worldcup.json을 그대로 복사해 둔 파일을 서버 시작 때 한 번 적재한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class WorldCupMatchLoader implements ApplicationRunner {

	private static final String COMPETITION = "FIFA World Cup 2026";
	private static final String RESOURCE = "data/worldcup-2026.json";

	private final MatchRepository matchRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws IOException {
		if (matchRepository.existsByCompetition(COMPETITION)) {
			return;
		}
		SourceFile file;
		try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
			file = objectMapper.readValue(in, SourceFile.class);
		}
		List<Match> matches = file.matches().stream().map(WorldCupMatchLoader::toMatch).toList();
		matchRepository.saveAll(matches);
		log.info("{} 경기 {}개를 적재했습니다.", COMPETITION, matches.size());
	}

	private static Match toMatch(SourceMatch m) {
		Score s = m.score();
		List<Integer> fin = s.et() != null ? s.et() : s.ft();
		return new Match(COMPETITION, m.round(), m.group(), kickoffAt(m.date(), m.time()), m.team1(), m.team2(),
				m.ground(), at(fin, 0), at(fin, 1), at(s.p(), 0), at(s.p(), 1));
	}

	// time 형식: "13:00 UTC-6"
	private static Instant kickoffAt(String date, String time) {
		String[] parts = time.split(" ");
		ZoneOffset offset = ZoneOffset.of(parts[1].substring("UTC".length()));
		return LocalDate.parse(date).atTime(LocalTime.parse(parts[0])).toInstant(offset);
	}

	private static Integer at(List<Integer> pair, int index) {
		return pair == null ? null : pair.get(index);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SourceFile(List<SourceMatch> matches) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record SourceMatch(String round, String date, String time, String team1, String team2, Score score, String group,
			String ground) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Score(List<Integer> ft, List<Integer> et, List<Integer> p) {
	}
}
