package dev.infreej.jeckathon.domain.room.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.room.dto.RoomResponse;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.global.response.PageResponse;
import lombok.RequiredArgsConstructor;

// 응원 팀 고르기 화면용. 한 경기에 열린 방을 만든 순서로 모두 준다. 팀별 구분은 응답의 team으로 한다.
@RestController
@RequestMapping("/api/matches/{matchId}/rooms")
@RequiredArgsConstructor
public class MatchRoomController {

	private final RoomService roomService;
	private final RoomSessionRegistry roomSessionRegistry;

	@GetMapping
	public PageResponse<RoomResponse> list(@PathVariable Long matchId) {
		List<RoomResponse> rooms = roomService.getRoomsOfMatch(matchId)
			.stream()
			.map(room -> RoomResponse.from(room, roomSessionRegistry.participantCount(room.getInviteCode())))
			.toList();
		return new PageResponse<>(rooms, 0, rooms.size(), rooms.size());
	}
}
