package com.groupsync.backend.badminton.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.CheckinResponses;
import com.groupsync.backend.badminton.model.BadmintonCheckinToken;
import com.groupsync.backend.badminton.model.BadmintonSessionStatus;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.repository.BadmintonCheckinTokenRepository;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.repository.BadmintonSessionRepository;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class CheckinService {
    private final BadmintonCheckinTokenRepository tokenRepository; private final BadmintonSessionRepository sessionRepository; private final BadmintonRegistrationRepository registrationRepository; private final MembershipRepository membershipRepository; private final UserAccountRepository userRepository;
    public CheckinService(BadmintonCheckinTokenRepository tokenRepository, BadmintonSessionRepository sessionRepository, BadmintonRegistrationRepository registrationRepository, MembershipRepository membershipRepository, UserAccountRepository userRepository) { this.tokenRepository = tokenRepository; this.sessionRepository = sessionRepository; this.registrationRepository = registrationRepository; this.membershipRepository = membershipRepository; this.userRepository = userRepository; }
    @Transactional public CheckinResponses.Token generate(AuthenticatedUser actor, Long sessionId) { var session = sessionRepository.findById(sessionId).orElseThrow(() -> new NotFoundException("Badminton session not found.")); requireOrganizer(actor, session.getGroup().getId()); String token = UUID.randomUUID().toString().replace("-", ""); Instant expires = session.getEndAt().isAfter(Instant.now()) ? session.getEndAt() : Instant.now().plusSeconds(3600); tokenRepository.save(new BadmintonCheckinToken(session, token, userRepository.findById(actor.getId()).orElseThrow(), expires)); return new CheckinResponses.Token(sessionId, session.getTitle(), token, "/check-in?token=" + token, expires); }
    @Transactional public CheckinResponses.Result checkIn(AuthenticatedUser actor, String token) { BadmintonCheckinToken checkinToken = tokenRepository.findByToken(token).orElseThrow(() -> new NotFoundException("Check-in token not found.")); var session = checkinToken.getSession(); if (Instant.now().isAfter(checkinToken.getExpiresAt())) throw new ConflictException("This check-in token has expired."); if (session.getStatus() != BadmintonSessionStatus.CONFIRMED && session.getStatus() != BadmintonSessionStatus.PLAYING) throw new ConflictException("This session is not accepting check-in."); membershipRepository.findByGroupIdAndUserId(session.getGroup().getId(), actor.getId()).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); var registration = registrationRepository.findBySessionIdAndUserId(session.getId(), actor.getId()).orElseThrow(() -> new ConflictException("You must register before checking in.")); if (registration.getStatus() == RegistrationStatus.CHECKED_IN) return new CheckinResponses.Result(session.getId(), session.getTitle(), registration.getStatus().name(), true); if (registration.getStatus() != RegistrationStatus.REGISTERED) throw new ConflictException("Only registered members can check in."); registration.checkIn(); return new CheckinResponses.Result(session.getId(), session.getTitle(), registration.getStatus().name(), false); }
    private void requireOrganizer(AuthenticatedUser actor, Long groupId) { membershipRepository.findByGroupIdAndUserId(groupId, actor.getId()).filter(m -> m.getRole() != GroupRole.MEMBER).orElseThrow(() -> new ForbiddenException("Only the owner or organizer can generate check-in tokens.")); }
}
