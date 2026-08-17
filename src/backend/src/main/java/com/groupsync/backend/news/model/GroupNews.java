package com.groupsync.backend.news.model;

import java.time.Instant;
import com.groupsync.backend.badminton.model.NewsType;
import com.groupsync.backend.group.model.Group;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity @Table(name = "group_news")
public class GroupNews {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id", nullable = false) private Group group;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "author_id") private UserAccount author;
    @Enumerated(EnumType.STRING) @Column(name = "news_type", nullable = false, length = 30) private NewsType type;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 1000) private String content;
    @Column(name = "source_key", length = 160, unique = true) private String sourceKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    protected GroupNews() { }
    public GroupNews(Group group, UserAccount author, NewsType type, String title, String content, String sourceKey) { this.group = group; this.author = author; this.type = type; this.title = title; this.content = content; this.sourceKey = sourceKey; }
    public Long getId() { return id; } public Group getGroup() { return group; } public UserAccount getAuthor() { return author; } public NewsType getType() { return type; } public String getTitle() { return title; } public String getContent() { return content; } public String getSourceKey() { return sourceKey; } public Instant getCreatedAt() { return createdAt; }
}
