package com.groupsync.backend.user.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_avatars")
public class UserAvatar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Column(name = "image_bytes", nullable = false)
    private byte[] imageBytes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected UserAvatar() {
    }

    public UserAvatar(UserAccount user, String contentType, byte[] imageBytes) {
        this.user = user;
        replace(contentType, imageBytes);
    }

    public void replace(String contentType, byte[] imageBytes) {
        this.contentType = contentType;
        this.imageBytes = imageBytes;
        this.updatedAt = Instant.now();
    }

    public String getContentType() { return contentType; }
    public byte[] getImageBytes() { return imageBytes; }
    public Instant getUpdatedAt() { return updatedAt; }
}
