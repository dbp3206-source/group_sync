package com.groupsync.backend.tournament.dto;

import java.util.List;

import com.groupsync.backend.tournament.model.TournamentCompetitionMode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class TournamentRequests {
    private TournamentRequests() {
    }

    public record Create(
        @NotBlank @Size(max = 160) String name,
        @NotNull Long seasonId,
        @NotNull Long sessionId,
        @NotNull TournamentCompetitionMode competitionMode,
        @Min(2) @Max(64) int maxEntries
    ) {
    }

    public record AddEntry(
        @Size(max = 180) String displayName,
        @NotEmpty List<@NotNull Long> memberIds,
        @Min(1) Integer seedNumber
    ) {
    }

    public record RecordWinner(@NotNull Long winnerEntryId) {
    }
}
