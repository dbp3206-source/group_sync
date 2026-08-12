package com.groupsync.backend.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.group.model.GroupType;
import com.groupsync.backend.study.model.StudySession;
import com.groupsync.backend.study.model.StudySessionStatus;
import com.groupsync.backend.user.model.UserAccount;

class StudySessionLifecycleTest {
    @Test
    void confirmedSessionCanBeRescheduledAndCancelledAsDerivedSourceChanges() {
        StudySession session = new StudySession(new Group("Study", null, GroupType.STUDY), new UserAccount("owner@example.com", "hash", "Owner"), "Topic", null, null, Instant.parse("2026-08-12T08:00:00Z"), Instant.parse("2026-08-12T09:00:00Z"), null);
        session.confirm();
        session.reschedule(Instant.parse("2026-08-12T10:00:00Z"), Instant.parse("2026-08-12T11:00:00Z"));

        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.CONFIRMED);
        assertThat(session.getStartAt()).isEqualTo(Instant.parse("2026-08-12T10:00:00Z"));
        session.cancel();
        assertThat(session.getStatus()).isEqualTo(StudySessionStatus.CANCELLED);
    }

    @Test
    void completedSessionCannotBeCancelled() {
        StudySession session = new StudySession(new Group("Study", null, GroupType.STUDY), new UserAccount("owner@example.com", "hash", "Owner"), "Topic", null, null, Instant.parse("2026-08-12T08:00:00Z"), Instant.parse("2026-08-12T09:00:00Z"), null);
        session.confirm();
        session.complete();

        assertThatThrownBy(session::cancel).isInstanceOf(IllegalStateException.class);
    }
}
