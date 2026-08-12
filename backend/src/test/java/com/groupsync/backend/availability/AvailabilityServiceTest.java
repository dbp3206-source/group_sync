package com.groupsync.backend.availability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.availability.dto.AvailabilityRequest;
import com.groupsync.backend.availability.service.AvailabilityService;
import com.groupsync.backend.availability.strategy.EarliestPossibleStrategy;
import com.groupsync.backend.availability.strategy.MaximumAttendanceStrategy;
import com.groupsync.backend.calendar.model.CalendarItem;
import com.groupsync.backend.calendar.model.CalendarSourceType;
import com.groupsync.backend.calendar.service.CalendarService;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupRole;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.group.model.Membership;
import com.groupsync.backend.group.repository.MembershipRepository;
import com.groupsync.backend.user.model.UserAccount;

class AvailabilityServiceTest {
    @Test
    void excludesBusySlotAndReturnsAvailableMembers() throws Exception {
        UserAccount owner = user(1L, "owner@example.com"); UserAccount member = user(2L, "member@example.com");
        Group group = new Group("Study", null, GroupType.STUDY);
        Membership ownerMembership = new Membership(group, owner, GroupRole.OWNER); Membership memberMembership = new Membership(group, member, GroupRole.MEMBER);
        MembershipRepository memberships = Mockito.mock(MembershipRepository.class); CalendarService calendar = Mockito.mock(CalendarService.class);
        when(memberships.findByGroupIdAndUserId(5L, 1L)).thenReturn(java.util.Optional.of(ownerMembership));
        when(memberships.findByGroupIdOrderByCreatedAtAsc(5L)).thenReturn(List.of(ownerMembership, memberMembership));
        when(calendar.getItemsForUser(1L, Instant.parse("2026-08-20T08:00:00Z"), Instant.parse("2026-08-20T11:00:00Z"))).thenReturn(List.of(new CalendarItem(CalendarSourceType.MANUAL, 1L, "Busy", Instant.parse("2026-08-20T08:00:00Z"), Instant.parse("2026-08-20T09:00:00Z"), true)));
        when(calendar.getItemsForUser(2L, Instant.parse("2026-08-20T08:00:00Z"), Instant.parse("2026-08-20T11:00:00Z"))).thenReturn(List.of());

        var result = new AvailabilityService(memberships, calendar, new MaximumAttendanceStrategy(), new EarliestPossibleStrategy()).find(AuthenticatedUser.from(owner), 5L, new AvailabilityRequest(Instant.parse("2026-08-20T08:00:00Z"), Instant.parse("2026-08-20T11:00:00Z"), 60, List.of(), 1, "MAXIMUM"));

        assertThat(result).hasSize(5);
        assertThat(result.getFirst().start()).isEqualTo(Instant.parse("2026-08-20T09:00:00Z"));
        assertThat(result.getFirst().availableMemberIds()).containsExactly(1L, 2L);
        assertThat(result).noneMatch(candidate -> candidate.start().equals(Instant.parse("2026-08-20T08:00:00Z")) && candidate.availableMemberIds().contains(1L));
    }

    private UserAccount user(Long id, String email) throws Exception { UserAccount user = new UserAccount(email, "hash", "Name"); Field field = UserAccount.class.getDeclaredField("id"); field.setAccessible(true); field.set(user, id); return user; }
}
