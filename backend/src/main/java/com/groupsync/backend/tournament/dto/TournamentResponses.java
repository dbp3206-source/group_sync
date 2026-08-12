package com.groupsync.backend.tournament.dto;
import java.time.Instant; import java.util.List;
import com.groupsync.backend.tournament.model.*;
public final class TournamentResponses {
    private TournamentResponses() { }
    public record Tournament(Long id, Long groupId, Long seasonId, Long sessionId, String name, String format, String status, int maxParticipants, Long championId, int participants) { public static Tournament from(com.groupsync.backend.tournament.model.Tournament t, int participants) { return new Tournament(t.getId(), t.getGroup().getId(), t.getSeason().getId(), t.getSession().getId(), t.getName(), t.getFormat(), t.getStatus().name(), t.getMaxParticipants(), t.getChampion() == null ? null : t.getChampion().getId(), participants); } }
    public record Participant(Long userId, String displayName, Integer seedNumber, Instant registeredAt) { public static Participant from(TournamentParticipant p) { return new Participant(p.getUser().getId(), p.getUser().getDisplayName(), p.getSeedNumber(), p.getRegisteredAt()); } }
    public record Bracket(Long id, String stage, int matchNumber, Integer nextMatchNumber, Long matchId, String status, Long winnerId) { public static Bracket from(TournamentMatch m) { return new Bracket(m.getId(), m.getStage().name(), m.getMatchNumber(), m.getNextMatchNumber(), m.getMatch().getId(), m.getMatch().getStatus().name(), m.getWinner() == null ? null : m.getWinner().getId()); } }
}
