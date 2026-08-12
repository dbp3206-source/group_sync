package com.groupsync.backend.badminton.dto;
import java.util.List;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
public final class MatchRequests {
    private MatchRequests() { }
    public record CreateMatchRequest(@NotNull Long courtId, @Min(1) int roundNumber, @NotEmpty List<Long> sideAUserIds, @NotEmpty List<Long> sideBUserIds) { }
    public record ScoreRequest(@Min(0) int scoreA, @Min(0) int scoreB) { }
}
