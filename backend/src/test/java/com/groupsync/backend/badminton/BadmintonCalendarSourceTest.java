package com.groupsync.backend.badminton;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.BadmintonSession;
import com.groupsync.backend.badminton.model.RegistrationStatus;
import com.groupsync.backend.badminton.model.Season;
import com.groupsync.backend.badminton.model.Venue;
import com.groupsync.backend.badminton.repository.BadmintonRegistrationRepository;
import com.groupsync.backend.badminton.service.BadmintonCalendarSource;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.user.model.UserAccount;

import org.mockito.Mockito;

class BadmintonCalendarSourceTest {
    @Test
    void onlyConfirmedActiveRegistrationAppearsAndCancellationDisappears() {
        Group group = new Group("Badminton", null, GroupType.BADMINTON);
        UserAccount user = new UserAccount("player@example.com", "hash", "Player");
        Season season = new Season(group, "Season 1", LocalDate.now(), null, true);
        Venue venue = new Venue(group, "Hall", null);
        Instant start = Instant.parse("2026-08-20T10:00:00Z");
        BadmintonSession session = new BadmintonSession(group, season, venue, "Play", start, start.plusSeconds(3600), start.minusSeconds(3600), 16, Set.of());
        session.open(); session.confirm();
        BadmintonRegistration registration = new BadmintonRegistration(session, user); registration.register(Instant.now());
        BadmintonRegistrationRepository repository = Mockito.mock(BadmintonRegistrationRepository.class);
        Mockito.when(repository.findByUserId(99L)).thenReturn(java.util.List.of(registration));
        BadmintonCalendarSource source = new BadmintonCalendarSource(repository);

        assertThat(source.getItems(99L, start.minusSeconds(60), start.plusSeconds(3660))).hasSize(1);
        registration.cancel();
        assertThat(source.getItems(99L, start.minusSeconds(60), start.plusSeconds(3660))).isEmpty();
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    }
}
