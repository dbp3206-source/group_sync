package com.groupsync.backend.knowledge.model;

import java.time.Instant;
import com.groupsync.backend.user.model.UserAccount;
import jakarta.persistence.*;

@Entity
@Table(name = "resources")
public class Resource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private UserAccount owner;
    @Column(nullable = false, length = 240) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(name = "resource_type", nullable = false, length = 20) private ResourceType resourceType;
    @Enumerated(EnumType.STRING) @Column(name = "processing_status", nullable = false, length = 20) private ResourceProcessingStatus processingStatus = ResourceProcessingStatus.UPLOADED;
    @Column(name = "original_filename", length = 512) private String originalFilename;
    @Column(name = "mime_type", length = 160) private String mimeType;
    @Column(name = "size_bytes") private Long sizeBytes;
    @Column(name = "storage_key", length = 512) private String storageKey;
    @Column(name = "checksum_sha256", length = 64) private String checksumSha256;
    @Column(nullable = false) private boolean favorite;
    @Column(nullable = false) private int priority;
    @Column(name = "processing_error", length = 500) private String processingError;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    protected Resource() { }
    public Resource(UserAccount owner, String title, String description, ResourceType type, String originalFilename, String mimeType, long sizeBytes, String storageKey, String checksum) {
        this.owner = owner; this.title = title; this.description = description; this.resourceType = type;
        this.originalFilename = originalFilename; this.mimeType = mimeType; this.sizeBytes = sizeBytes; this.storageKey = storageKey; this.checksumSha256 = checksum;
    }
    @PreUpdate void touch() { updatedAt = Instant.now(); }
    public void updateMetadata(String title, String description, boolean favorite, int priority) { this.title = title; this.description = description; this.favorite = favorite; this.priority = priority; }
    public void beginParsing() { transition(ResourceProcessingStatus.UPLOADED, ResourceProcessingStatus.PARSING); }
    public void beginChunking() { transition(ResourceProcessingStatus.PARSING, ResourceProcessingStatus.CHUNKING); }
    public void beginEmbedding() { transition(ResourceProcessingStatus.CHUNKING, ResourceProcessingStatus.EMBEDDING); }
    public void markReady() {
        if (processingStatus != ResourceProcessingStatus.EMBEDDING && processingStatus != ResourceProcessingStatus.PARSING && processingStatus != ResourceProcessingStatus.CHUNKING) {
            throw new IllegalStateException("Resource is not ready for ready.");
        }
        processingStatus = ResourceProcessingStatus.READY;
        processingError = null;
    }
    public void markFailed(String message) { processingStatus = ResourceProcessingStatus.FAILED; processingError = message == null ? "Processing could not be completed." : message.substring(0, Math.min(500, message.length())); }
    public void retry() { if (processingStatus != ResourceProcessingStatus.FAILED) throw new IllegalStateException("Only failed resources can be retried."); processingStatus = ResourceProcessingStatus.UPLOADED; processingError = null; }
    private void transition(ResourceProcessingStatus expected, ResourceProcessingStatus next) { if (processingStatus != expected) throw new IllegalStateException("Resource is not ready for " + next.name().toLowerCase() + "."); processingStatus = next; }
    public Long getId() { return id; } public UserAccount getOwner() { return owner; } public String getTitle() { return title; } public String getDescription() { return description; } public ResourceType getResourceType() { return resourceType; } public ResourceProcessingStatus getProcessingStatus() { return processingStatus; } public String getOriginalFilename() { return originalFilename; } public String getMimeType() { return mimeType; } public Long getSizeBytes() { return sizeBytes; } public String getStorageKey() { return storageKey; } public String getChecksumSha256() { return checksumSha256; } public boolean isFavorite() { return favorite; } public int getPriority() { return priority; } public String getProcessingError() { return processingError; } public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
