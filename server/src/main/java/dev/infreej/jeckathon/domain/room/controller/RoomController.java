package dev.infreej.jeckathon.domain.room.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.infreej.jeckathon.domain.room.dto.RoomCreateRequest;
import dev.infreej.jeckathon.domain.room.dto.RoomResponse;
import dev.infreej.jeckathon.domain.room.dto.RoomStatusMessage;
import dev.infreej.jeckathon.domain.room.entity.Room;
import dev.infreej.jeckathon.domain.room.entity.RoomStatus;
import dev.infreej.jeckathon.domain.room.service.RoomService;
import dev.infreej.jeckathon.domain.room.service.RoomSessionRegistry;
import dev.infreej.jeckathon.global.error.BusinessException;
import dev.infreej.jeckathon.global.error.ErrorCode;
import dev.infreej.jeckathon.global.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 방 생성·시작·종료는 권한 검사 없이 열어 둔다.
// 상태 변경 알림은 서비스 트랜잭션이 커밋된 뒤에 보내려고 여기서 보낸다.
@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

	private static final int MAX_PAGE_SIZE = 100;

	private final RoomService roomService;
	private final RoomSessionRegistry roomSessionRegistry;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public RoomResponse create(@Valid @RequestBody RoomCreateRequest request) {
		return toResponse(roomService.create(request.matchId(), request.name(), request.team(), request.capacity()));
	}

	// 로비 목록. status를 생략하면 진행 중, 대기, 종료 순으로 전체를 준다.
	@GetMapping
	public PageResponse<RoomResponse> list(@RequestParam(required = false) RoomStatus status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST);
		}
		return PageResponse.from(roomService.getRooms(status, page, size), this::toResponse);
	}

	@GetMapping("/{inviteCode}")
	public RoomResponse get(@PathVariable String inviteCode) {
		return toResponse(roomService.getByInviteCode(inviteCode));
	}

	@PostMapping("/{inviteCode}/start")
	public RoomResponse start(@PathVariable String inviteCode) {
		Room room = roomService.start(inviteCode);
		roomSessionRegistry.broadcast(room.getInviteCode(), RoomStatusMessage.from(room));
		return toResponse(room);
	}

	@PostMapping("/{inviteCode}/end")
	public RoomResponse end(@PathVariable String inviteCode) {
		Room room = roomService.end(inviteCode);
		roomSessionRegistry.broadcast(room.getInviteCode(), RoomStatusMessage.from(room));
		return toResponse(room);
	}

	private RoomResponse toResponse(Room room) {
		return RoomResponse.from(room, roomSessionRegistry.participantCount(room.getInviteCode()));
	}
}
