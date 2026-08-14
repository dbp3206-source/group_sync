package com.groupsync.backend.tournament.dto;

import java.time.Instant;
import java.util.List;

import com.groupsync.backend.tournament.model.Tournament;
import com.groupsync.backend.tournament.model.TournamentEntry;
import com.groupsync.backend.tournament.model.TournamentMatch;

public final class TournamentResponses {
    private TournamentResponses() {
    }

    public record Tournament(
        Long id,
        Long groupId,
        Long seasonId,
        Long sessionId,
        String name,
        String competitionMode,
        String status,
        int maxEntries,
        Long championEntryId,
        int entries
    ) {
        public static Tournament from(com.groupsync.backend.tournament.model.Tournament tournament, int entries) {
            return new Tournament(tournament.getId(), tournament.getGroup().getId(), tournament.getSeason().getId(), tournament.getSession().getId(), tournament.getName(), tournament.getCompetitionMode().name(), tournament.getStatus().name(), tournament.getMaxParticipants(), tournament.getChampionEntry() == null ? null : tournament.getChampionEntry().getId(), entries);
        }
    }

    public record EntryMember(Long userId, String displayName) {
    }

    public record Entry(Long id, String displayName, Integer seedNumber, Instant createdAt, List<EntryMember> members) {
        public static Entry from(TournamentEntry entry) {
            return new Entry(entry.getId(), entry.getDisplayName(), entry.getSeedNumber(), entry.getCreatedAt(), entry.getMembers().stream().map(member -> new EntryMember(member.getUser().getId(), member.getUser().getDisplayName())).toList());
        }
    }

    public record Bracket(Long id, String stage, int matchNumber, Integer nextMatchNumber, Entry entryA, Entry entryB, Entry winnerEntry, String status) {
        public static Bracket from(TournamentMatch match) {
            String status = match.getWinnerEntry() != null ? "COMPLETED" : match.getEntryA() != null && match.getEntryB() != null ? "READY" : "PENDING";
            return new Bracket(match.getId(), match.getStage().name(), match.getMatchNumber(), match.getNextMatchNumber(), match.getEntryA() == null ? null : Entry.from(match.getEntryA()), match.getEntryB() == null ? null : Entry.from(match.getEntryB()), match.getWinnerEntry() == null ? null : Entry.from(match.getWinnerEntry()), status);
        }
    }
}
