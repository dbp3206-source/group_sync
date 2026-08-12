package com.groupsync.backend.badminton.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.AssignResponsibilityRequest;
import com.groupsync.backend.badminton.dto.BadmintonResponses;
import com.groupsync.backend.badminton.dto.CreateCourtRequest;
import com.groupsync.backend.badminton.dto.CreateResponsibilityRequest;
import com.groupsync.backend.badminton.dto.CreateSessionRequest;
import com.groupsync.backend.badminton.dto.CreateVenueRequest;
import com.groupsync.backend.badminton.dto.ProfileRequest;
import com.groupsync.backend.badminton.dto.RescheduleSessionRequest;
import com.groupsync.backend.badminton.model.BadmintonProfile;
import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.BadmintonSessionStatus;
import com.groupsync.backend.badminton.model.Court;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.model.SessionResponsibility;
import com.groupsync.backend.badminton.model.Venue;
import com.groupsync.backend.badminton.repository.BadmintonProfileRepository;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.badminton.repository.CourtRepository;
import com.groupsync.backend.badminton.repository.ResponsibilityRepository;
import com.groupsync.backend.badminton.repository.SeasonRepository;
import com.groupsync.backend.badminton.repository.VenueRepository;
import com.groupsync.backend.calendar.aggregation.CalendarAggregatorService;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.GroupRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.notification.service.NotificationService;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class BadmintonService {
    private static final int DEFAULT_CAPACITY = 16;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final UserAccountRepository userRepository;
    private final SeasonRepository seasonRepository;
    private final BadmintonProfileRepository profileRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;
    private final BadmintonSessionRepository sessionRepository;
    private final BadmintonRegistrationRepository registrationRepository;
    private final ResponsibilityRepository responsibilityRepository;
    private final CalendarAggregatorService calendarAggregator;
    private final NotificationService notificationService;

    public BadmintonService(GroupRepository groupRepository, MembershipRepository membershipRepository, UserAccountRepository userRepository,
        SeasonRepository seasonRepository, BadmintonProfileRepository profileRepository, VenueRepository venueRepository, CourtRepository courtRepository,
        BadmintonSessionRepository sessionRepository, BadmintonRegistrationRepository registrationRepository, ResponsibilityRepository responsibilityRepository,
        CalendarAggregatorService calendarAggregator, NotificationService notificationService) {
        this.groupRepository = groupRepository; this.membershipRepository = membershipRepository; this.userRepository = userRepository;
        this.seasonRepository = seasonRepository; this.profileRepository = profileRepository; this.venueRepository = venueRepository;
        this.courtRepository = courtRepository; this.sessionRepository = sessionRepository; this.registrationRepository = registrationRepository;
        this.responsibilityRepository = responsibilityRepository; this.calendarAggregator = calendarAggregator; this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<BadmintonResponses.SeasonResponse> seasons(AuthenticatedUser actor, Long groupId) {
        requireMember(groupId, actor.getId()); requireBadminton(groupId);
        return seasonRepository.findByGroupIdOrderByStartsOnDesc(groupId).stream().map(BadmintonResponses.SeasonResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<BadmintonResponses.VenueResponse> venues(AuthenticatedUser actor, Long groupId) {
        requireMember(groupId, actor.getId()); requireBadminton(groupId);
        return venueRepository.findByGroupIdOrderByNameAsc(groupId).stream().map(v ->
            BadmintonResponses.VenueResponse.from(v, courtRepository.findByVenueIdOrderByNameAsc(v.getId()))).toList();
    }

    @Transactional
    public BadmintonResponses.VenueResponse createVenue(AuthenticatedUser actor, Long groupId, CreateVenueRequest request) {
        requireOrganizer(groupId, actor.getId()); requireBadminton(groupId);
        Venue venue = new Venue(findGroup(groupId), request.name().trim(), clean(request.address()));
        return BadmintonResponses.VenueResponse.from(venueRepository.save(venue), List.of());
    }

    @Transactional
    public BadmintonResponses.CourtResponse createCourt(AuthenticatedUser actor, Long groupId, Long venueId, CreateCourtRequest request) {
        requireOrganizer(groupId, actor.getId()); requireBadminton(groupId);
        var venue = venueRepository.findByIdAndGroupId(venueId, groupId).orElseThrow(() -> new NotFoundException("Venue not found."));
        return BadmintonResponses.CourtResponse.from(courtRepository.save(new Court(venue, request.name().trim())));
    }

    @Transactional
    public BadmintonResponses.ProfileResponse updateProfile(AuthenticatedUser actor, Long groupId, ProfileRequest request) {
        Membership membership = requireMember(groupId, actor.getId()); requireBadminton(groupId);
        BadmintonProfile profile = profileRepository.findByMembershipId(membership.getId()).orElse(null);
        if (profile == null) profile = profileRepository.save(new BadmintonProfile(membership, request.skillLevel(), clean(request.bio())));
        else profile.update(request.skillLevel(), clean(request.bio()));
        return BadmintonResponses.ProfileResponse.from(profile);
    }

    @Transactional(readOnly = true)
    public BadmintonResponses.ProfileResponse profile(AuthenticatedUser actor, Long groupId, Long userId) {
        requireMember(groupId, actor.getId()); requireBadminton(groupId);
        Membership membership = membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new NotFoundException("Group member not found."));
        return profileRepository.findByMembershipId(membership.getId()).map(BadmintonResponses.ProfileResponse::from)
            .orElseThrow(() -> new NotFoundException("Badminton profile not found."));
    }

    @Transactional(readOnly = true)
    public List<BadmintonResponses.SessionResponse> sessions(AuthenticatedUser actor, Long groupId) {
        requireMember(groupId, actor.getId()); requireBadminton(groupId);
        return sessionRepository.findByGroupIdOrderByStartAtDesc(groupId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BadmintonResponses.SessionResponse createSession(AuthenticatedUser actor, Long groupId, CreateSessionRequest request) {
        requireOrganizer(groupId, actor.getId()); requireBadminton(groupId);
        Instant start = request.start().toInstant(), end = request.end().toInstant(), deadline = request.registrationDeadline().toInstant();
        validateTimes(start, end, deadline);
        Season season = seasonRepository.findById(request.seasonId()).filter(s -> s.getGroup().getId().equals(groupId))
            .orElseThrow(() -> new NotFoundException("Season not found."));
        var venue = venueRepository.findByIdAndGroupId(request.venueId(), groupId).orElseThrow(() -> new NotFoundException("Venue not found."));
        Set<Court> courts = request.courtIds().stream().map(id -> courtRepository.findById(id).orElseThrow(() -> new NotFoundException("Court not found."))).collect(Collectors.toSet());
        if (courts.stream().anyMatch(c -> !c.getVenue().getId().equals(venue.getId()) || !c.isActive())) throw new ConflictException("Every selected court must be active and belong to the selected venue.");
        int capacity = request.capacity() == null ? DEFAULT_CAPACITY : request.capacity();
        return toResponse(sessionRepository.save(new BadmintonSession(findGroup(groupId), season, venue, request.title().trim(), start, end, deadline, capacity, courts)));
    }

    @Transactional public BadmintonResponses.SessionResponse open(AuthenticatedUser actor, Long id) { BadmintonSession s = organizerSession(actor, id); s.open(); return toResponse(s); }
    @Transactional public BadmintonResponses.SessionResponse confirm(AuthenticatedUser actor, Long id) { BadmintonSession s = organizerSession(actor, id); s.confirm(); notifyRegistered(s, "SESSION_CONFIRMED", "Badminton session confirmed", "The session is confirmed and added to your calendar."); return toResponse(s); }
    @Transactional public BadmintonResponses.SessionResponse cancel(AuthenticatedUser actor, Long id) { BadmintonSession s = organizerSession(actor, id); s.cancel(); notifyRegistered(s, "SESSION_CANCELLED", "Badminton session cancelled", s.getTitle() + " was cancelled."); return toResponse(s); }
    @Transactional public BadmintonResponses.SessionResponse reschedule(AuthenticatedUser actor, Long id, RescheduleSessionRequest request) { BadmintonSession s = organizerSession(actor, id); Instant start = request.start().toInstant(), end = request.end().toInstant(), deadline = request.registrationDeadline().toInstant(); validateTimes(start, end, deadline); s.reschedule(start, end, deadline); notifyRegistered(s, "SESSION_CHANGED", "Badminton session changed", "The time of " + s.getTitle() + " changed; your calendar was updated."); return toResponse(s); }

    @Transactional
    public BadmintonResponses.SessionResponse join(AuthenticatedUser actor, Long id) {
        BadmintonSession s = sessionRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Badminton session not found."));
        requireMember(s.getGroup().getId(), actor.getId());
        if (s.getStatus() != BadmintonSessionStatus.OPEN && s.getStatus() != BadmintonSessionStatus.CONFIRMED) throw new ConflictException("This session is not open for registration.");
        if (!Instant.now().isBefore(s.getRegistrationDeadline())) throw new ConflictException("Registration deadline has passed.");
        BadmintonRegistration r = registrationRepository.findBySessionIdAndUserId(id, actor.getId()).orElseGet(() -> new BadmintonRegistration(s, userRepository.findById(actor.getId()).orElseThrow(() -> new NotFoundException("User not found."))));
        if (r.getStatus() == RegistrationStatus.REGISTERED || r.getStatus() == RegistrationStatus.WAITLISTED || r.getStatus() == RegistrationStatus.CHECKED_IN) throw new ConflictException("You already have an active registration.");
        boolean conflict = !calendarAggregator.getItems(actor.getId(), s.getStartAt(), s.getEndAt()).isEmpty();
        if (registrationRepository.countActiveBySessionId(id) < s.getCapacity()) r.register(Instant.now()); else r.waitlist(Instant.now());
        registrationRepository.save(r);
        return toResponse(s, conflict, actor.getId());
    }

    @Transactional
    public BadmintonResponses.SessionResponse cancelRegistration(AuthenticatedUser actor, Long id) {
        BadmintonSession s = sessionRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Badminton session not found."));
        requireMember(s.getGroup().getId(), actor.getId());
        BadmintonRegistration r = registrationRepository.findBySessionIdAndUserId(id, actor.getId()).orElseThrow(() -> new NotFoundException("Registration not found."));
        boolean active = r.isActive(); r.cancel(); if (active) promoteNext(s); return toResponse(s);
    }

    @Transactional
    public BadmintonResponses.SessionResponse checkIn(AuthenticatedUser actor, Long id, Long userId, boolean noShow) {
        BadmintonSession s = organizerSession(actor, id);
        BadmintonRegistration r = registrationRepository.findBySessionIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException("Registration not found."));
        if (noShow) r.noShow(); else r.checkIn();
        return toResponse(s);
    }

    @Transactional
    public BadmintonResponses.ResponsibilityResponse addResponsibility(AuthenticatedUser actor, Long id, CreateResponsibilityRequest request) {
        BadmintonSession s = organizerSession(actor, id);
        return BadmintonResponses.ResponsibilityResponse.from(responsibilityRepository.save(new SessionResponsibility(s, request.itemName().trim(), clean(request.note()))));
    }

    @Transactional
    public BadmintonResponses.ResponsibilityResponse assignResponsibility(AuthenticatedUser actor, Long id, Long responsibilityId, AssignResponsibilityRequest request) {
        BadmintonSession s = organizerSession(actor, id);
        SessionResponsibility r = responsibilityRepository.findById(responsibilityId).filter(x -> x.getSession().getId().equals(s.getId())).orElseThrow(() -> new NotFoundException("Responsibility not found."));
        Membership assignee = membershipRepository.findByGroupIdAndUserId(s.getGroup().getId(), request.userId()).orElseThrow(() -> new NotFoundException("Assignee is not a group member."));
        r.assign(assignee.getUser()); return BadmintonResponses.ResponsibilityResponse.from(r);
    }

    @Transactional
    public BadmintonResponses.ResponsibilityResponse unassignResponsibility(AuthenticatedUser actor, Long responsibilityId) {
        SessionResponsibility r = responsibilityRepository.findById(responsibilityId).orElseThrow(() -> new NotFoundException("Responsibility not found."));
        organizerSession(actor, r.getSession().getId()); r.unassign(); return BadmintonResponses.ResponsibilityResponse.from(r);
    }

    private void promoteNext(BadmintonSession s) { registrationRepository.findOldestWaitlisted(s.getId()).ifPresent(r -> { r.promote(Instant.now()); notificationService.create(r.getUser().getId(), "WAITLIST_PROMOTED", "You moved off the waitlist", "A place opened in " + s.getTitle() + ".", "BADMINTON_SESSION", s.getId()); }); }
    private void notifyRegistered(BadmintonSession s, String type, String title, String message) { registrationRepository.findBySessionIdAndStatusInOrderByRegisteredAtAscIdAsc(s.getId(), List.of(RegistrationStatus.REGISTERED, RegistrationStatus.CHECKED_IN)).forEach(r -> notificationService.create(r.getUser().getId(), type, title, message, "BADMINTON_SESSION", s.getId())); }
    private BadmintonResponses.SessionResponse toResponse(BadmintonSession s) { return toResponse(s, false, null); }
    private BadmintonResponses.SessionResponse toResponse(BadmintonSession s, boolean conflict, Long actorId) { return BadmintonResponses.SessionResponse.from(s, registrationRepository.findBySessionIdOrderByRegisteredAtAscIdAsc(s.getId()).stream().map(r -> BadmintonResponses.RegistrationResponse.from(r, conflict && r.getStatus() == RegistrationStatus.REGISTERED && r.getUser().getId().equals(actorId))).toList(), responsibilityRepository.findBySessionIdOrderByItemNameAsc(s.getId()).stream().map(BadmintonResponses.ResponsibilityResponse::from).toList()); }
    private BadmintonSession organizerSession(AuthenticatedUser actor, Long id) { BadmintonSession s = sessionRepository.findById(id).orElseThrow(() -> new NotFoundException("Badminton session not found.")); requireOrganizer(s.getGroup().getId(), actor.getId()); return s; }
    private Membership requireOrganizer(Long groupId, Long userId) { Membership m = requireMember(groupId, userId); if (m.getRole() == GroupRole.MEMBER) throw new ForbiddenException("Only the owner or an organizer can manage badminton operations."); return m; }
    private Membership requireMember(Long groupId, Long userId) { return membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
    private void requireBadminton(Long groupId) { if (findGroup(groupId).getType() != GroupType.BADMINTON) throw new ConflictException("This operation is only available for BADMINTON groups."); }
    private Group findGroup(Long id) { return groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Group not found.")); }
    private void validateTimes(Instant start, Instant end, Instant deadline) { if (!start.isBefore(end)) throw new BadRequestException("Session end must be after start."); if (deadline.isAfter(start)) throw new BadRequestException("Registration deadline must be before session start."); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
