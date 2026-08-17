package com.groupsync.backend.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.groupsync.backend.study.model.StudyParticipant;
import com.groupsync.backend.study.model.StudySession;
import com.groupsync.backend.study.service.StudyCalendarSource;
import com.groupsync.backend.study.repository.StudyParticipantRepository;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.user.model.UserAccount;

class StudyCalendarSourceTest {
    @Test
    void sourceReflectsRescheduleAndRemovesCancelledSessionWithoutSyncRow() throws Exception {
        UserAccount user = user(1L);
        StudySession session = new StudySession(new Group("Study", null, GroupType.STUDY), user, "Topic", null, null, Instant.parse("2026-08-21T08:00:00Z"), Instant.parse("2026-08-21T09:00:00Z"), null);
        session.confirm();
        StudyParticipant participant = new StudyParticipant(session, user);
        StudyParticipantRepository repository = Mockito.mock(StudyParticipantRepository.class);
        when(repository.findByUserId(1L)).thenReturn(List.of(participant));
        StudyCalendarSource source = new StudyCalendarSource(repository);

        assertThat(source.getItems(1L, Instant.parse("2026-08-21T00:00:00Z"), Instant.parse("2026-08-22T00:00:00Z"))).hasSize(1);
        session.reschedule(Instant.parse("2026-08-21T10:00:00Z"), Instant.parse("2026-08-21T11:00:00Z"));
        assertThat(source.getItems(1L, Instant.parse("2026-08-21T00:00:00Z"), Instant.parse("2026-08-22T00:00:00Z")).getFirst().start()).isEqualTo(Instant.parse("2026-08-21T10:00:00Z"));
        session.cancel();
        assertThat(source.getItems(1L, Instant.parse("2026-08-21T00:00:00Z"), Instant.parse("2026-08-22T00:00:00Z"))).isEmpty();
    }

    private UserAccount user(Long id) throws Exception {
        UserAccount user = new UserAccount("calendar@example.com", "hash", "Calendar");
        Field field = UserAccount.class.getDeclaredField("id"); field.setAccessible(true); field.set(user, id); return user;
    }
}
