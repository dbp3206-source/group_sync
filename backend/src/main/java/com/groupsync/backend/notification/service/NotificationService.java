package com.groupsync.backend.notification.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.badminton.dto.BadmintonResponses.NotificationResponse;
import com.groupsync.backend.notification.model.Notification;
import com.groupsync.backend.notification.model.NotificationPreference;
import com.groupsync.backend.notification.repository.NotificationRepository;
import com.groupsync.backend.notification.repository.NotificationPreferenceRepository;
import com.groupsync.backend.notification.dto.NotificationPreferenceResponse;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    public NotificationService(NotificationRepository notificationRepository, UserAccountRepository userRepository, NotificationPreferenceRepository preferenceRepository) { this.notificationRepository = notificationRepository; this.userRepository = userRepository; this.preferenceRepository = preferenceRepository; }
    @Transactional public void create(Long userId, String type, String title, String message, String targetType, Long targetId) { if (!enabled(userId, type)) return; UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found.")); notificationRepository.save(new Notification(user, type, title, message, targetType, targetId)); }
    @Transactional public void createOnce(Long userId, String type, String title, String message, String targetType, Long targetId, String sourceKey) { if (!enabled(userId, type) || notificationRepository.findBySourceKeyAndUserId(sourceKey, userId).isPresent()) return; UserAccount user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found.")); notificationRepository.save(new Notification(user, type, title, message, targetType, targetId, sourceKey)); }
    @Transactional(readOnly = true) public List<NotificationResponse> list(AuthenticatedUser actor) { return notificationRepository.findByUserIdOrderByCreatedAtDesc(actor.getId()).stream().map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getTargetType(), n.getTargetId(), n.isRead(), n.getCreatedAt())).toList(); }
    @Transactional public void markRead(AuthenticatedUser actor, Long id) { notificationRepository.findByIdAndUserId(id, actor.getId()).orElseThrow(() -> new NotFoundException("Notification not found.")).markRead(); }
    @Transactional(readOnly = true) public List<NotificationPreferenceResponse> preferences(AuthenticatedUser actor) { return preferenceRepository.findByUserIdOrderByTypeAsc(actor.getId()).stream().map(NotificationPreferenceResponse::from).toList(); }
    @Transactional public NotificationPreferenceResponse setPreference(AuthenticatedUser actor, String type, boolean enabled) { UserAccount user = userRepository.findById(actor.getId()).orElseThrow(() -> new NotFoundException("User not found.")); NotificationPreference preference = preferenceRepository.findByUserIdAndType(actor.getId(), type).orElseGet(() -> new NotificationPreference(user, type, enabled)); preference.setEnabled(enabled); return NotificationPreferenceResponse.from(preferenceRepository.save(preference)); }
    private boolean enabled(Long userId, String type) { return preferenceRepository.findByUserIdAndType(userId, type).map(NotificationPreference::isEnabled).orElse(true); }
}
