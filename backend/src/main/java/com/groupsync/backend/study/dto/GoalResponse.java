package com.groupsync.backend.study.dto;

import com.groupsync.backend.study.model.StudyGoal;

public record GoalResponse(Long id, String description, boolean completed) {
    public static GoalResponse from(StudyGoal goal) { return new GoalResponse(goal.getId(), goal.getDescription(), goal.isCompleted()); }
}
