package com.groupsync.backend.badminton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.BadmintonSessionStatus;
import com.groupsync.backend.badminton.model.Court;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.model.SessionResponsibility;
import com.groupsync.backend.badminton.model.Venue;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.user.model.UserAccount;

class BadmintonOperationsRulesTest {
    private final Instant start = Instant.parse("2026-08-20T10:00:00Z");

    @Test
    void sessionLifecycleProtectsTheOperationalOrder() {
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        Season season = new Season(group, "Season 1", LocalDate.now(), null, true);
        Venue venue = new Venue(group, "Court hall", null);
        BadmintonSession session = new BadmintonSession(group, season, venue, "Thursday play", start, start.plusSeconds(7200), start.minusSeconds(3600), 16, Set.of());

        assertThat(session.getStatus()).isEqualTo(BadmintonSessionStatus.DRAFT);
        session.open(); session.confirm(); session.start(); session.complete();
        assertThatThrownBy(session::cancel).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void duplicateRegistrationIsRejectedByTheUniqueBusinessState() {
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        UserAccount user = new UserAccount("player@example.com", "hash", "Player");
        BadmintonSession session = session(group);
        BadmintonRegistration registration = new BadmintonRegistration(session, user);
        registration.register(Instant.now());

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        registration.checkIn();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThatThrownBy(registration::checkIn).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelledRegisteredPlayerPromotesTheOldestWaitlistedPlayer() {
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        UserAccount first = new UserAccount("first@example.com", "hash", "First");
        UserAccount second = new UserAccount("second@example.com", "hash", "Second");
        BadmintonSession session = session(group);
        BadmintonRegistration registered = new BadmintonRegistration(session, first);
        BadmintonRegistration waitlisted = new BadmintonRegistration(session, second);
        Instant now = Instant.now();
        registered.register(now); waitlisted.waitlist(now.plusSeconds(1));

        registered.cancel();
        waitlisted.promote(now.plusSeconds(2));

        assertThat(registered.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(waitlisted.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
    }

    @Test
    void unassignedResponsibilityReturnsToNeeded() {
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        UserAccount user = new UserAccount("player@example.com", "hash", "Player");
        SessionResponsibility responsibility = new SessionResponsibility(session(group), "Shuttlecock", null);

        responsibility.assign(user);
        responsibility.unassign();

        assertThat(responsibility.getStatus()).isEqualTo(com.groupsync.backend.badminton.model.ResponsibilityStatus.NEEDED);
        assertThat(responsibility.getAssignee()).isNull();
    }

    private BadmintonSession session(Group group) {
        Season season = new Season(group, "Season 1", LocalDate.now(), null, true);
        Venue venue = new Venue(group, "Court hall", null);
        BadmintonSession session = new BadmintonSession(group, season, venue, "Open play", start, start.plusSeconds(3600), start.minusSeconds(3600), 1, Set.of());
        session.open();
        return session;
    }
}
