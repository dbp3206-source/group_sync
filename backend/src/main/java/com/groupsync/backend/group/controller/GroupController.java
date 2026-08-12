package com.groupsync.backend.group.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.group.dto.ChangeMemberRoleRequest;
import com.groupsync.backend.group.dto.CreateGroupRequest;
import com.groupsync.backend.group.dto.GroupDetailResponse;
import com.groupsync.backend.group.dto.GroupSummaryResponse;
import com.groupsync.backend.group.dto.InvitationResponse;
import com.groupsync.backend.group.dto.InviteUserRequest;
import com.groupsync.backend.group.service.GroupService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<GroupSummaryResponse> myGroups(@AuthenticationPrincipal AuthenticatedUser actor) {
        return groupService.listMyGroups(actor);
    }

    @PostMapping
    public ResponseEntity<GroupDetailResponse> create(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @Valid @RequestBody CreateGroupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(actor, request));
    }

    @GetMapping("/{groupId}")
    public GroupDetailResponse detail(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable Long groupId) {
        return groupService.getGroup(actor, groupId);
    }

    @PostMapping("/{groupId}/invitations")
    public InvitationResponse invite(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long groupId,
        @Valid @RequestBody InviteUserRequest request
    ) {
        return groupService.invite(actor, groupId, request);
    }

    @GetMapping("/invitations/pending")
    public List<InvitationResponse> pendingInvitations(@AuthenticationPrincipal AuthenticatedUser actor) {
        return groupService.listPendingInvitations(actor);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public InvitationResponse acceptInvitation(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long invitationId
    ) {
        return groupService.acceptInvitation(actor, invitationId);
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public InvitationResponse declineInvitation(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long invitationId
    ) {
        return groupService.declineInvitation(actor, invitationId);
    }

    @PatchMapping("/{groupId}/members/{userId}/role")
    public GroupDetailResponse changeRole(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long groupId,
        @PathVariable Long userId,
        @Valid @RequestBody ChangeMemberRoleRequest request
    ) {
        return groupService.changeMemberRole(actor, groupId, userId, request);
    }

    @PostMapping("/{groupId}/transfer-ownership/{userId}")
    public GroupDetailResponse transferOwnership(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long groupId,
        @PathVariable Long userId
    ) {
        return groupService.transferOwnership(actor, groupId, userId);
    }

    @DeleteMapping("/{groupId}/members/me")
    public ResponseEntity<Void> leave(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @PathVariable Long groupId
    ) {
        groupService.leaveGroup(actor, groupId);
        return ResponseEntity.noContent().build();
    }
}
