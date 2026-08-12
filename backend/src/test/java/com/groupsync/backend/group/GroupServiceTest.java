package com.groupsync.backend.group;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.group.dto.InviteUserRequest;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.GroupRepository;
import com.groupsync.backend.group.repository.InvitationRepository;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.group.service.GroupService;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {
    @Mock private GroupRepository groupRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private InvitationRepository invitationRepository;
    @Mock private UserAccountRepository userRepository;

    @Test
    void memberCannotInviteAnotherUser() throws Exception {
        UserAccount actorAccount = user(1L, "member@example.com");
        AuthenticatedUser actor = AuthenticatedUser.from(actorAccount);
        Group group = new Group("Study", null, GroupType.STUDY);
        Membership membership = new Membership(group, actorAccount, GroupRole.MEMBER);
        when(membershipRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        GroupService service = service();

        assertThatThrownBy(() -> service.invite(actor, 10L, new InviteUserRequest("other@example.com")))
            .isInstanceOf(ForbiddenException.class);
        verify(invitationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ownerCannotLeaveWithoutTransferringOwnership() throws Exception {
        UserAccount actorAccount = user(1L, "owner@example.com");
        AuthenticatedUser actor = AuthenticatedUser.from(actorAccount);
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        Membership membership = new Membership(group, actorAccount, GroupRole.OWNER);
        when(membershipRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service().leaveGroup(actor, 10L))
            .isInstanceOf(ConflictException.class);
        verify(membershipRepository, never()).delete(membership);
    }

    private GroupService service() {
        return new GroupService(groupRepository, membershipRepository, invitationRepository, userRepository);
    }

    private UserAccount user(Long id, String email) throws Exception {
        UserAccount user = new UserAccount(email, "hash", "Name");
        Field field = UserAccount.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        return user;
    }
}
