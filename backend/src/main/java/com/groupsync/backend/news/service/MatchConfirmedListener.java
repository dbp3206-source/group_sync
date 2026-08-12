package com.groupsync.backend.news.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.groupsync.backend.badminton.event.MatchConfirmedEvent;
import com.groupsync.backend.notification.service.NotificationService;

@Component
public class MatchConfirmedListener {
    private final NewsService newsService; private final NotificationService notificationService;
    public MatchConfirmedListener(NewsService newsService, NotificationService notificationService) { this.newsService = newsService; this.notificationService = notificationService; }
    @EventListener public void onMatchConfirmed(MatchConfirmedEvent event) { String sourceKey = "MATCH_RESULT:" + event.matchId(); newsService.createSystem(event.groupId(), "New badminton result", event.title() + " finished " + event.score() + ".", sourceKey); for (Long userId : event.participantIds()) notificationService.createOnce(userId, "MATCH_RESULT", "Result confirmed", "A badminton result was confirmed: " + event.score() + ".", "BADMINTON_MATCH", event.matchId(), sourceKey); }
}
