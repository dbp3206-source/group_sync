package com.groupsync.backend.badminton.dto;

import java.time.Instant;

public final class CheckinResponses {
    private CheckinResponses() { }
    public record Token(Long sessionId, String sessionTitle, String token, String checkInUrl, Instant expiresAt) { }
    public record Result(Long sessionId, String sessionTitle, String status, boolean alreadyCheckedIn) { }
    public record CheckinRequest(String token) { }
}
