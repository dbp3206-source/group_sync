package com.groupsync.backend.group.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.group.dto.ChangeMemberRoleRequest;
import com.groupsync.backend.group.dto.CreateGroupRequest;
import com.groupsync.backend.group.dto.GroupDetailResponse;
import com.groupsync.backend.group.dto.GroupMemberResponse;
import com.groupsync.backend.group.dto.GroupSummaryResponse;
import com.groupsync.backend.group.dto.InvitationResponse;
import com.groupsync.backend.group.dto.InviteUserRequest;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.Invitation;
import com.groupsync.backend.group.model.InvitationStatus;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.GroupRepository;
import com.groupsync.backend.group.repository.InvitationRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final InvitationRepository invitationRepository;
    private final UserAccountRepository userRepository;

    public GroupService(
        GroupRepository groupRepository,
        MembershipRepository membershipRepository,
        InvitationRepository invitationRepository,
        UserAccountRepository userRepository
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GroupDetailResponse createGroup(AuthenticatedUser actor, CreateGroupRequest request) {
        Group group = groupRepository.save(new Group(request.name().trim(), normalizeDescription(request.description()), request.type()));
        UserAccount user = findUser(actor.getId());
        membershipRepository.save(new Membership(group, user, GroupRole.OWNER));
        return getGroup(actor, group.getId());
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listMyGroups(AuthenticatedUser actor) {
        return membershipRepository.findByUserIdOrderByCreatedAtDesc(actor.getId()).stream()
            .map(GroupSummaryResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(AuthenticatedUser actor, Long groupId) {
        requireMembership(groupId, actor.getId());
        Group group = findGroup(groupId);
        List<GroupMemberResponse> members = membershipRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
            .map(GroupMemberResponse::from)
            .toList();
        return GroupDetailResponse.of(group, members);
    }

    @Transactional
    public InvitationResponse invite(AuthenticatedUser actor, Long groupId, InviteUserRequest request) {
        requireOrganizer(groupId, actor.getId());
        Group group = findGroup(groupId);
        UserAccount invitee = userRepository.findByEmail(request.email().trim().toLowerCase())
            .orElseThrow(() -> new NotFoundException("The invited user does not exist."));
        if (invitee.getId().equals(actor.getId())) {
            throw new ConflictException("You are already the group owner or member.");
        }
        if (membershipRepository.existsByGroupIdAndUserId(groupId, invitee.getId())) {
            throw new ConflictException("This user is already a group member.");
        }
        if (invitationRepository.existsByGroupIdAndInviteeIdAndStatus(groupId, invitee.getId(), InvitationStatus.PENDING)) {
            throw new ConflictException("A pending invitation already exists for this user.");
        }
        Invitation invitation = invitationRepository.save(new Invitation(group, invitee, findUser(actor.getId())));
        return InvitationResponse.from(invitation);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPendingInvitations(AuthenticatedUser actor) {
        return invitationRepository.findByInviteeIdAndStatusOrderByCreatedAtDesc(actor.getId(), InvitationStatus.PENDING).stream()
            .map(InvitationResponse::from)
            .toList();
    }

    @Transactional
    public InvitationResponse acceptInvitation(AuthenticatedUser actor, Long invitationId) {
        Invitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Invitation not found."));
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            return InvitationResponse.from(invitation);
        }
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("This invitation is no longer pending.");
        }
        if (!membershipRepository.existsByGroupIdAndUserId(invitation.getGroup().getId(), actor.getId())) {
            membershipRepository.save(new Membership(invitation.getGroup(), findUser(actor.getId()), GroupRole.MEMBER));
        }
        invitation.accept();
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public InvitationResponse declineInvitation(AuthenticatedUser actor, Long invitationId) {
        Invitation invitation = invitationRepository.findByIdAndInviteeId(invitationId, actor.getId())
            .orElseThrow(() -> new NotFoundException("Invitation not found."));
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ConflictException("This invitation is no longer pending.");
        }
        invitation.decline();
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public GroupDetailResponse changeMemberRole(AuthenticatedUser actor, Long groupId, Long userId, ChangeMemberRoleRequest request) {
        Membership owner = requireOwner(groupId, actor.getId());
        Membership target = membershipRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new NotFoundException("Group member not found."));
        if (request.role() == GroupRole.OWNER) {
            throw new ConflictException("Use ownership transfer to change the owner.");
        }
        if (target.getRole() == GroupRole.OWNER || owner.getUser().getId().equals(target.getUser().getId())) {
            throw new ConflictException("The owner cannot change their own role here.");
        }
        target.changeRole(request.role());
        return getGroup(actor, groupId);
    }

    @Transactional
    public GroupDetailResponse transferOwnership(AuthenticatedUser actor, Long groupId, Long userId) {
        Membership owner = requireOwner(groupId, actor.getId());
        Membership target = membershipRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new NotFoundException("Group member not found."));
        if (target.getUser().getId().equals(owner.getUser().getId())) {
            throw new ConflictException("Ownership is already assigned to this user.");
        }
        owner.changeRole(GroupRole.MEMBER);
        target.changeRole(GroupRole.OWNER);
        return getGroup(actor, groupId);
    }

    @Transactional
    public void leaveGroup(AuthenticatedUser actor, Long groupId) {
        Membership membership = requireMembership(groupId, actor.getId());
        if (membership.getRole() == GroupRole.OWNER) {
            throw new ConflictException("The owner must transfer ownership before leaving the group.");
        }
        membershipRepository.delete(membership);
    }

    private Membership requireOrganizer(Long groupId, Long userId) {
        Membership membership = requireMembership(groupId, userId);
        if (membership.getRole() != GroupRole.OWNER && membership.getRole() != GroupRole.ORGANIZER) {
            throw new ForbiddenException("Only the owner or an organizer can perform this action.");
        }
        return membership;
    }

    private Membership requireOwner(Long groupId, Long userId) {
        Membership membership = requireMembership(groupId, userId);
        if (membership.getRole() != GroupRole.OWNER) {
            throw new ForbiddenException("Only the group owner can perform this action.");
        }
        return membership;
    }

    private Membership requireMembership(Long groupId, Long userId) {
        return membershipRepository.findByGroupIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ForbiddenException("You are not a member of this group."));
    }

    private Group findGroup(Long groupId) {
        return groupRepository.findById(groupId)
            .orElseThrow(() -> new NotFoundException("Group not found."));
    }

    private UserAccount findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found."));
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
