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
import dev.infreej.jeckathon.domain.room.dto.RoomStatusMessage;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.service.RoomEndService;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 방 생성·시작·종료는 권한 검사 없이 열어 둔다.
// 상태 변경 알림은 서비스 트랜잭션이 커밋된 뒤에 보내려고 여기서 보낸다.
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

	private final RoomService roomService;
	private final RoomEndService roomEndService;
	private final RoomSessionRegistry roomSessionRegistry;

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
		Room room = roomService.start(inviteCode);
		roomSessionRegistry.broadcast(room.getInviteCode(), RoomStatusMessage.from(room));
		return RoomResponse.from(room);
	}

	@PostMapping("/{inviteCode}/end")
	public RoomResponse end(@PathVariable String inviteCode) {
		Room room = roomEndService.end(inviteCode);
		roomSessionRegistry.broadcast(room.getInviteCode(), RoomStatusMessage.from(room));
		return RoomResponse.from(room);
	}
}
