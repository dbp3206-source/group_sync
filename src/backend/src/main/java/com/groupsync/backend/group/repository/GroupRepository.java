package com.groupsync.backend.group.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.groupsync.backend.group.model.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {
}
