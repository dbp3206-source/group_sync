package com.groupsync.backend.notification.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.notification.model.NotificationPreference;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    List<NotificationPreference> findByUserIdOrderByTypeAsc(Long userId);
    Optional<NotificationPreference> findByUserIdAndType(Long userId, String type);
}
