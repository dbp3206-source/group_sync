package com.groupsync.backend.study.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.GroupRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.study.dto.AttendanceRequest;
import com.groupsync.backend.study.dto.CreateStudyGoalRequest;
import com.groupsync.backend.study.dto.CreateStudyMaterialRequest;
import com.groupsync.backend.study.dto.CreateStudySessionRequest;
import com.groupsync.backend.study.dto.GoalResponse;
import com.groupsync.backend.study.dto.MaterialResponse;
import com.groupsync.backend.study.dto.ParticipantResponse;
import com.groupsync.backend.study.dto.RescheduleStudySessionRequest;
import com.groupsync.backend.study.dto.StudySessionResponse;
import com.groupsync.backend.study.model.StudyGoal;
import com.groupsync.backend.study.model.StudyMaterial;
import com.groupsync.backend.study.model.StudyParticipant;
import com.groupsync.backend.study.model.StudySession;
import com.groupsync.backend.study.model.StudySessionStatus;
import com.groupsync.backend.study.repository.StudyGoalRepository;
import com.groupsync.backend.study.repository.StudyMaterialRepository;
import com.groupsync.backend.study.repository.StudyParticipantRepository;
import com.groupsync.backend.study.repository.StudySessionRepository;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class StudyService {
    private final StudySessionRepository sessionRepository;
    private final StudyParticipantRepository participantRepository;
    private final StudyMaterialRepository materialRepository;
    private final StudyGoalRepository goalRepository;
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final UserAccountRepository userRepository;

    public StudyService(StudySessionRepository sessionRepository, StudyParticipantRepository participantRepository, StudyMaterialRepository materialRepository, StudyGoalRepository goalRepository, GroupRepository groupRepository, MembershipRepository membershipRepository, UserAccountRepository userRepository) {
        this.sessionRepository = sessionRepository; this.participantRepository = participantRepository; this.materialRepository = materialRepository; this.goalRepository = goalRepository; this.groupRepository = groupRepository; this.membershipRepository = membershipRepository; this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudySessionResponse> list(AuthenticatedUser actor, Long groupId) {
        requireMembership(groupId, actor.getId());
        return sessionRepository.findByGroupIdOrderByStartAtAsc(groupId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public StudySessionResponse get(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireMembership(session.getGroup().getId(), actor.getId());
        return response(session);
    }

    @Transactional
    public StudySessionResponse create(AuthenticatedUser actor, Long groupId, CreateStudySessionRequest request) {
        requireOrganizer(groupId, actor.getId());
        validateTime(request.start(), request.end());
        Group group = findGroup(groupId);
        if (group.getType() != GroupType.STUDY) throw new BadRequestException("Study sessions require a STUDY group.");
        UserAccount organizer = findUser(actor.getId());
        StudySession session = sessionRepository.save(new StudySession(group, organizer, request.topic().trim(), clean(request.goal()), clean(request.location()), request.start(), request.end(), request.capacity()));
        participantRepository.save(new StudyParticipant(session, organizer));
        return response(session);
    }

    @Transactional
    public StudySessionResponse join(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireMembership(session.getGroup().getId(), actor.getId());
        if (session.getStatus() != StudySessionStatus.OPEN) throw new ConflictException("Only open study sessions can be joined.");
        if (participantRepository.existsBySessionIdAndUserId(sessionId, actor.getId())) throw new ConflictException("You already joined this study session.");
        if (session.getCapacity() != null && participantRepository.countBySessionId(sessionId) >= session.getCapacity()) throw new ConflictException("Study session capacity is full.");
        participantRepository.save(new StudyParticipant(session, findUser(actor.getId())));
        return response(session);
    }

    @Transactional
    public void leave(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireMembership(session.getGroup().getId(), actor.getId());
        StudyParticipant participant = participantRepository.findBySessionIdAndUserId(sessionId, actor.getId()).orElseThrow(() -> new NotFoundException("Study participant not found."));
        if (session.getOrganizer().getId().equals(actor.getId())) throw new ConflictException("The organizer cannot leave the study session.");
        if (session.getStatus() != StudySessionStatus.OPEN) throw new ConflictException("Only open study sessions can be left.");
        participantRepository.delete(participant);
    }

    @Transactional
    public StudySessionResponse confirm(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireOrganizer(session.getGroup().getId(), actor.getId());
        if (participantRepository.countBySessionId(sessionId) == 0) throw new ConflictException("A study session needs at least one participant.");
        session.confirm();
        return response(session);
    }

    @Transactional
    public StudySessionResponse reschedule(AuthenticatedUser actor, Long sessionId, RescheduleStudySessionRequest request) {
        StudySession session = findSession(sessionId);
        requireOrganizer(session.getGroup().getId(), actor.getId());
        validateTime(request.start(), request.end());
        if (session.getStatus() == StudySessionStatus.CANCELLED || session.getStatus() == StudySessionStatus.COMPLETED) throw new ConflictException("This study session cannot be rescheduled.");
        session.reschedule(request.start(), request.end());
        return response(session);
    }

    @Transactional
    public StudySessionResponse cancel(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireOrganizer(session.getGroup().getId(), actor.getId());
        session.cancel();
        return response(session);
    }

    @Transactional
    public StudySessionResponse complete(AuthenticatedUser actor, Long sessionId) {
        StudySession session = findSession(sessionId);
        requireOrganizer(session.getGroup().getId(), actor.getId());
        session.complete();
        return response(session);
    }

    @Transactional
    public StudySessionResponse addMaterial(AuthenticatedUser actor, Long sessionId, CreateStudyMaterialRequest request) {
        StudySession session = findSession(sessionId); requireOrganizer(session.getGroup().getId(), actor.getId());
        materialRepository.save(new StudyMaterial(session, request.title().trim(), request.url().trim())); return response(session);
    }

    @Transactional
    public StudySessionResponse addGoal(AuthenticatedUser actor, Long sessionId, CreateStudyGoalRequest request) {
        StudySession session = findSession(sessionId); requireOrganizer(session.getGroup().getId(), actor.getId());
        goalRepository.save(new StudyGoal(session, request.description().trim())); return response(session);
    }

    @Transactional
    public StudySessionResponse toggleGoal(AuthenticatedUser actor, Long sessionId, Long goalId) {
        StudySession session = findSession(sessionId); requireMembership(session.getGroup().getId(), actor.getId());
        StudyGoal goal = goalRepository.findById(goalId).filter(item -> item.getSession().getId().equals(sessionId)).orElseThrow(() -> new NotFoundException("Study goal not found."));
        goal.toggle(); return response(session);
    }

    @Transactional
    public StudySessionResponse markAttendance(AuthenticatedUser actor, Long sessionId, Long userId, AttendanceRequest request) {
        StudySession session = findSession(sessionId); requireOrganizer(session.getGroup().getId(), actor.getId());
        StudyParticipant participant = participantRepository.findBySessionIdAndUserId(sessionId, userId).orElseThrow(() -> new NotFoundException("Study participant not found."));
        participant.markAttendance(request.attendance()); return response(session);
    }

    private StudySessionResponse response(StudySession session) {
        return StudySessionResponse.of(session, participantRepository.findBySessionIdOrderByJoinedAtAsc(session.getId()).stream().map(ParticipantResponse::from).toList(), materialRepository.findBySessionIdOrderByIdAsc(session.getId()).stream().map(MaterialResponse::from).toList(), goalRepository.findBySessionIdOrderByIdAsc(session.getId()).stream().map(GoalResponse::from).toList());
    }
    private StudySession findSession(Long id) { return sessionRepository.findById(id).orElseThrow(() -> new NotFoundException("Study session not found.")); }
    private Group findGroup(Long id) { return groupRepository.findById(id).orElseThrow(() -> new NotFoundException("Group not found.")); }
    private UserAccount findUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found.")); }
    private Membership requireMembership(Long groupId, Long userId) { return membershipRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(() -> new ForbiddenException("You are not a member of this group.")); }
    private void requireOrganizer(Long groupId, Long userId) { Membership membership = requireMembership(groupId, userId); if (membership.getRole() != GroupRole.OWNER && membership.getRole() != GroupRole.ORGANIZER) throw new ForbiddenException("Only the owner or an organizer can manage study sessions."); }
    private void validateTime(Instant start, Instant end) { if (start == null || end == null || !start.isBefore(end)) throw new BadRequestException("Study session end must be after start."); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
