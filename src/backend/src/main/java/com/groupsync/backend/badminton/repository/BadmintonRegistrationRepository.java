package com.groupsync.backend.badminton.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.groupsync.backend.badminton.model.BadmintonRegistration;
import com.groupsync.backend.badminton.model.RegistrationStatus;

public interface BadmintonRegistrationRepository extends JpaRepository<BadmintonRegistration, Long> {
    List<BadmintonRegistration> findBySessionIdOrderByRegisteredAtAscIdAsc(Long sessionId);
    List<BadmintonRegistration> findBySessionIdAndStatusInOrderByRegisteredAtAscIdAsc(Long sessionId, Collection<RegistrationStatus> statuses);
    List<BadmintonRegistration> findBySessionIdAndStatusOrderByQueuedAtAscIdAsc(Long sessionId, RegistrationStatus status);
    List<BadmintonRegistration> findByUserId(Long userId);
    Optional<BadmintonRegistration> findBySessionIdAndUserId(Long sessionId, Long userId);

    @Query("select count(r) from BadmintonRegistration r where r.session.id = :sessionId and r.status in (com.groupsync.backend.badminton.model.RegistrationStatus.REGISTERED, com.groupsync.backend.badminton.model.RegistrationStatus.CHECKED_IN)")
    long countActiveBySessionId(Long sessionId);

    default Optional<BadmintonRegistration> findOldestWaitlisted(Long sessionId) {
        return findBySessionIdAndStatusOrderByQueuedAtAscIdAsc(sessionId, RegistrationStatus.WAITLISTED).stream().findFirst();
    }
}
