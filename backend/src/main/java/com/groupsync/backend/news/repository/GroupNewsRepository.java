package com.groupsync.backend.news.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.groupsync.backend.news.model.GroupNews;
public interface GroupNewsRepository extends JpaRepository<GroupNews, Long> { List<GroupNews> findByGroupIdOrderByCreatedAtDesc(Long groupId); Optional<GroupNews> findBySourceKey(String sourceKey); }
