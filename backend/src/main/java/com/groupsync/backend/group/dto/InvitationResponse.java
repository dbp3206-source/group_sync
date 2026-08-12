package com.groupsync.backend.group.dto;

import com.groupsync.backend.group.model.Invitation;

public record InvitationResponse(
    Long id,
    Long groupId,
    String groupName,
    String inviteeEmail,
    String inviterDisplayName,
    String status
) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
            invitation.getId(),
            invitation.getGroup().getId(),
            invitation.getGroup().getName(),
            invitation.getInvitee().getEmail(),
            invitation.getInviter().getDisplayName(),
            invitation.getStatus().name());
    }
}
