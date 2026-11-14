package dev.infreej.jeckathon.domain.room.service;

public record RoomEndedEvent(Long roomId, String inviteCode) {
}
