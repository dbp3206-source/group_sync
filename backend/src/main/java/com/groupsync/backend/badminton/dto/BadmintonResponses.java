package com.groupsync.backend.badminton.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.groupsync.backend.badminton.model.BadmintonProfile;
import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.Court;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.model.SessionResponsibility;
import com.groupsync.backend.badminton.model.Venue;

public final class BadmintonResponses {
    private BadmintonResponses() { }
    public record SeasonResponse(Long id, String name, LocalDate startsOn, LocalDate endsOn, boolean active) {
        public static SeasonResponse from(Season s) { return new SeasonResponse(s.getId(), s.getName(), s.getStartsOn(), s.getEndsOn(), s.isActive()); }
    }
    public record CourtResponse(Long id, String name, boolean active) { public static CourtResponse from(Court c) { return new CourtResponse(c.getId(), c.getName(), c.isActive()); } }
    public record VenueResponse(Long id, String name, String address, List<CourtResponse> courts) { public static VenueResponse from(Venue v, List<Court> courts) { return new VenueResponse(v.getId(), v.getName(), v.getAddress(), courts.stream().map(CourtResponse::from).toList()); } }
    public record ProfileResponse(Long membershipId, Long userId, String displayName, String skillLevel, String bio) { public static ProfileResponse from(BadmintonProfile p) { return new ProfileResponse(p.getMembership().getId(), p.getMembership().getUser().getId(), p.getMembership().getUser().getDisplayName(), p.getSkillLevel().name(), p.getBio()); } }
    public record RegistrationResponse(Long id, Long userId, String displayName, String status, Instant queuedAt, boolean conflictWarning) { public static RegistrationResponse from(BadmintonRegistration r, boolean conflict) { return new RegistrationResponse(r.getId(), r.getUser().getId(), r.getUser().getDisplayName(), r.getStatus().name(), r.getQueuedAt(), conflict); } }
    public record ResponsibilityResponse(Long id, String itemName, String status, Long assigneeId, String assigneeName, String note) { public static ResponsibilityResponse from(SessionResponsibility r) { return new ResponsibilityResponse(r.getId(), r.getItemName(), r.getStatus().name(), r.getAssignee() == null ? null : r.getAssignee().getId(), r.getAssignee() == null ? null : r.getAssignee().getDisplayName(), r.getNote()); } }
    public record SessionResponse(Long id, Long groupId, String title, Instant start, Instant end, Instant registrationDeadline, int capacity, String status, Long venueId, String venueName, Long seasonId, String seasonName, List<CourtResponse> courts, List<RegistrationResponse> registrations, List<ResponsibilityResponse> responsibilities) {
        public static SessionResponse from(BadmintonSession s, List<RegistrationResponse> registrations, List<ResponsibilityResponse> responsibilities) { return new SessionResponse(s.getId(), s.getGroup().getId(), s.getTitle(), s.getStartAt(), s.getEndAt(), s.getRegistrationDeadline(), s.getCapacity(), s.getStatus().name(), s.getVenue().getId(), s.getVenue().getName(), s.getSeason().getId(), s.getSeason().getName(), s.getCourts().stream().map(CourtResponse::from).toList(), registrations, responsibilities); }
    }
    public record NotificationResponse(Long id, String type, String title, String message, String targetType, Long targetId, boolean read, Instant createdAt) { }
}
