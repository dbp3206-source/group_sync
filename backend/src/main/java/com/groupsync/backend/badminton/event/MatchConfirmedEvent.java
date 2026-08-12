package com.groupsync.backend.badminton.event;
import java.util.List;
public record MatchConfirmedEvent(Long matchId, Long groupId, String title, String score, List<Long> participantIds) { }
