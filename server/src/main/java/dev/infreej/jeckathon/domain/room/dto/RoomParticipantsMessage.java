package dev.infreej.jeckathon.domain.room.dto;

// 웹소켓: { "type": "room.participants", "roomId": "<inviteCode>", "participantCount": 3 }
public record RoomParticipantsMessage(String type, String roomId, int participantCount) {
}
