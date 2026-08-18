package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import com.groupsync.backend.user.model.UserAccount;

@Entity
@Table(name = "study_topics")
public class StudyTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserAccount owner;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";

    @ManyToMany
    @JoinTable(
        name = "study_topic_resources",
        joinColumns = @JoinColumn(name = "topic_id"),
        inverseJoinColumns = @JoinColumn(name = "resource_id")
    )
    private Set<Resource> resources = new HashSet<>();

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private Set<TopicConcept> concepts = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudyTopic() {}

    public StudyTopic(UserAccount owner, String title, String goal) {
        this.owner = owner;
        this.title = title;
        this.goal = goal;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getOwner() { return owner; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; this.updatedAt = Instant.now(); }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; this.updatedAt = Instant.now(); }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public Set<Resource> getResources() { return resources; }
    public Set<TopicConcept> getConcepts() { return concepts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void addResource(Resource resource) {
        this.resources.add(resource);
        this.updatedAt = Instant.now();
    }

    public void removeResource(Resource resource) {
        this.resources.remove(resource);
        this.updatedAt = Instant.now();
    }
}
