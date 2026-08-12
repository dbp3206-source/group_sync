package com.groupsync.backend.study.dto;

import java.time.Instant;
import java.util.List;

import com.groupsync.backend.study.model.StudySession;

public record StudySessionResponse(
    Long id, Long groupId, String topic, String goal, String location, Instant start, Instant end, Integer capacity, String status,
    List<ParticipantResponse> participants, List<MaterialResponse> materials, List<GoalResponse> goals
) {
    public static StudySessionResponse of(StudySession session, List<ParticipantResponse> participants, List<MaterialResponse> materials, List<GoalResponse> goals) {
        return new StudySessionResponse(session.getId(), session.getGroup().getId(), session.getTopic(), session.getGoal(), session.getLocation(), session.getStartAt(), session.getEndAt(), session.getCapacity(), session.getStatus().name(), participants, materials, goals);
    }
}
