package dev.infreej.jeckathon.domain.room.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.infreej.jeckathon.domain.match.entity.Match;
import dev.infreej.jeckathon.domain.match.service.MatchService;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.repository.RoomRepository;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

	// 헷갈리는 글자(0, O, 1, I, l)를 뺀 초대 코드 문자
	private static final String INVITE_CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
	private static final int INVITE_CODE_LENGTH = 8;

	private final RoomRepository roomRepository;
	private final MatchService matchService;
	private final SecureRandom random = new SecureRandom();

	@Transactional
	public Room create(Long matchId, String name, String team, Integer capacity) {
		Match match = matchService.getMatch(matchId);
		String roomTeam = team == null || team.isBlank() ? null : team.strip();
		if (roomTeam != null && !roomTeam.equals(match.getTeam1()) && !roomTeam.equals(match.getTeam2())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "team은 경기의 두 팀 중 하나여야 합니다.");
		}
		String roomName = name == null || name.isBlank() ? match.getTeam1() + " vs " + match.getTeam2() : name.strip();
		return roomRepository.save(new Room(generateInviteCode(), roomName, match, roomTeam, capacity));
	}

	// 응원 팀 고르기 화면용. 같은 경기의 방을 만든 순서로 준다.
	public List<Room> getRoomsOfMatch(Long matchId) {
		matchService.getMatch(matchId);
		return roomRepository.findAllByMatchIdOrderByCreatedAtAscIdAsc(matchId);
	}

	// 로비 목록. status가 없으면 진행 중, 대기, 종료 순으로 전체를 준다.
	public Page<Room> getRooms(RoomStatus status, int page, int size) {
		PageRequest pageable = PageRequest.of(page, size);
		if (status == null) {
			return roomRepository.findAllForLobby(pageable);
		}
		return roomRepository.findAllByStatusOrderByCreatedAtDesc(status, pageable);
	}

	// 다른 도메인이 방을 찾을 때도 이 메서드를 쓴다.
	public Room getByInviteCode(String inviteCode) {
		return roomRepository.findByInviteCode(inviteCode)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	// 거래·코멘트처럼 경기 중에만 받는 요청에서 쓴다.
	public Room getLiveByInviteCode(String inviteCode) {
		Room room = getByInviteCode(inviteCode);
		if (!room.isAcceptingTrades()) {
			throw new BusinessException(ErrorCode.ROOM_NOT_LIVE);
		}
		return room;
	}

	// 1초 집계 대상인 방 목록
	public List<Room> getLiveRooms() {
		return roomRepository.findAllByStatusAndEndedAtIsNull(RoomStatus.LIVE);
	}

	@Transactional
	public Room start(String inviteCode) {
		Room room = getByInviteCodeForUpdate(inviteCode);
		if (room.getStatus() != RoomStatus.WAITING) {
			throw new BusinessException(ErrorCode.ROOM_INVALID_STATUS, "대기 중인 방만 시작할 수 있습니다.");
		}
		room.start(Instant.now());
		return room;
	}

	@Transactional
	public Room beginEnd(String inviteCode) {
		Room room = getByInviteCodeForUpdate(inviteCode);
		if (room.getStatus() == RoomStatus.WAITING) {
			throw new BusinessException(ErrorCode.ROOM_INVALID_STATUS, "진행 중인 방만 종료할 수 있습니다.");
		}
		if (room.isAcceptingTrades()) {
			room.beginEnd(Instant.now());
		}
		return room;
	}

	@Transactional
	public Room completeEnd(String inviteCode, double changeRate, double volatility, Double replayScore) {
		Room room = getByInviteCodeForUpdate(inviteCode);
		if (room.getStatus() != RoomStatus.ENDED) {
			room.completeEnd(changeRate, volatility, replayScore);
		}
		return room;
	}

	public Room getByInviteCodeForUpdate(String inviteCode) {
		return roomRepository.findByInviteCodeForUpdate(inviteCode)
			.orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
	}

	private String generateInviteCode() {
		String code;
		do {
			StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
			for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
				builder.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
			}
			code = builder.toString();
		} while (roomRepository.existsByInviteCode(code));
		return code;
	}
}
