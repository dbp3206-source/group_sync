package com.groupsync.backend.study.dto;

import com.groupsync.backend.study.model.StudyParticipant;

public record ParticipantResponse(Long userId, String displayName, String email, String attendance) {
    public static ParticipantResponse from(StudyParticipant participant) { return new ParticipantResponse(participant.getUser().getId(), participant.getUser().getDisplayName(), participant.getUser().getEmail(), participant.getAttendance().name()); }
}
