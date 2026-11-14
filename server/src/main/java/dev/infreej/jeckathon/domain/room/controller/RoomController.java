package dev.infreej.jeckathon.domain.room.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.room.dto.RoomCreateRequest;
import dev.infreej.jeckathon.domain.room.dto.RoomResponse;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 방 생성·시작·종료는 권한 검사 없이 열어 둔다.
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoomResponse create(@Valid @RequestBody RoomCreateRequest request) {
		return RoomResponse.from(roomService.create(request.name()));
	}

	@GetMapping("/{inviteCode}")
	public RoomResponse get(@PathVariable String inviteCode) {
		return RoomResponse.from(roomService.getByInviteCode(inviteCode));
	}

	@PostMapping("/{inviteCode}/start")
	public RoomResponse start(@PathVariable String inviteCode) {
		return RoomResponse.from(roomService.start(inviteCode));
	}

	@PostMapping("/{inviteCode}/end")
	public RoomResponse end(@PathVariable String inviteCode) {
		return RoomResponse.from(roomService.end(inviteCode));
	}
}
