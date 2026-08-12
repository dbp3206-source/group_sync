package com.groupsync.backend.badminton;

import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.groupsync.backend.badminton.event.MatchConfirmedEvent;
import com.groupsync.backend.news.service.MatchConfirmedListener;
import com.groupsync.backend.news.service.NewsService;
import com.groupsync.backend.notification.service.NotificationService;

class MatchConfirmedListenerTest {
    @Test
    void confirmedMatchCreatesSystemNewsAndParticipantNotifications() {
        NewsService news = Mockito.mock(NewsService.class);
        NotificationService notifications = Mockito.mock(NotificationService.class);
        MatchConfirmedListener listener = new MatchConfirmedListener(news, notifications);
        listener.onMatchConfirmed(new MatchConfirmedEvent(4L, 2L, "Saturday badminton", "21-17", List.of(7L, 8L)));
        verify(news).createSystem(2L, "New badminton result", "Saturday badminton finished 21-17.", "MATCH_RESULT:4");
        verify(notifications).createOnce(7L, "MATCH_RESULT", "Result confirmed", "A badminton result was confirmed: 21-17.", "BADMINTON_MATCH", 4L, "MATCH_RESULT:4");
        verify(notifications).createOnce(8L, "MATCH_RESULT", "Result confirmed", "A badminton result was confirmed: 21-17.", "BADMINTON_MATCH", 4L, "MATCH_RESULT:4");
    }
}
