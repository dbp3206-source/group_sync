package com.groupsync.backend.notification.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.BadmintonResponses.NotificationResponse;
import com.groupsync.backend.notification.model.Notification;
import com.groupsync.backend.notification.repository.NotificationRepository;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userRepository;
    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userRepository) { this.notificationRepository = notificationRepository; this.userRepository = userRepository; }
    @Transactional public void create(Long userId, String type, String title, String message, String targetType, Long targetId) { UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found.")); notificationRepository.save(new Notification(user, type, title, message, targetType, targetId)); }
    @Transactional public void createOnce(Long userId, String type, String title, String message, String targetType, Long targetId, String sourceKey) { if (notificationRepository.findBySourceKeyAndUserId(sourceKey, userId).isPresent()) return; UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found.")); notificationRepository.save(new Notification(user, type, title, message, targetType, targetId, sourceKey)); }
    @Transactional(readOnly = true) public List<NotificationResponse> list(AuthenticatedUser actor) { return notificationRepository.findByUserIdOrderByCreatedAtDesc(actor.getId()).stream().map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getTargetType(), n.getTargetId(), n.isRead(), n.getCreatedAt())).toList(); }
    @Transactional public void markRead(AuthenticatedUser actor, Long id) { notificationRepository.findByIdAndUserId(id, actor.getId()).orElseThrow(() -> new NotFoundException("Notification not found.")).markRead(); }
}
