package com.groupsync.backend.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.user.model.UserAvatar;

public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {
    Optional<UserAvatar> findByUserId(Long userId);
    void deleteByUserId(Long userId);
}
