package com.groupsync.backend.tournament.dto;
import jakarta.validation.constraints.*;
import com.groupsync.backend.badminton.dto.MatchRequests.CreateMatchRequest;
public final class TournamentRequests {
    private TournamentRequests() { }
    public record Create(@NotBlank @Size(max=160) String name, @NotNull Long seasonId, @NotNull Long sessionId, @NotBlank String format, @Min(2) @Max(64) int maxParticipants) { }
    public record AddParticipant(@NotNull Long userId) { }
    public record CreateTournamentMatch(@NotNull String stage, @Min(1) int matchNumber, Integer nextMatchNumber, @NotNull CreateMatchRequest match) { }
}
