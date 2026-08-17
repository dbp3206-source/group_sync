package com.groupsync.backend.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.user.dto.ChangePasswordRequest;
import com.groupsync.backend.user.dto.UpdateProfileRequest;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.model.UserAvatar;
import com.groupsync.backend.user.repository.UserAccountRepository;
import com.groupsync.backend.user.repository.UserAvatarRepository;

@Service
public class UserProfileService {
    private static final int MAX_AVATAR_BYTES = 512 * 1024;

    private final UserAccountRepository userRepository;
    private final UserAvatarRepository avatarRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
        UserAccountRepository userRepository,
        UserAvatarRepository avatarRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserAccount getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
    }

    @Transactional
    public UserAccount updateProfile(Long userId, UpdateProfileRequest request) {
        UserAccount user = getUser(userId);
        try {
            ZoneId.of(request.timeZone());
        } catch (RuntimeException exception) {
            throw new BadRequestException("Time zone is not recognized.");
        }
        user.updateProfile(request.displayName().trim(), request.timeZone());
        return user;
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserAccount user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ForbiddenException("Current password is incorrect.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public UserAvatar saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload.");
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new BadRequestException("Avatar must be 512 KB or smaller.");
        }
        try {
            byte[] imageBytes = file.getBytes();
            String contentType = detectImageType(imageBytes);
            UserAccount user = getUser(userId);
            UserAvatar avatar = avatarRepository.findByUserId(userId)
                .orElseGet(() -> new UserAvatar(user, contentType, imageBytes));
            avatar.replace(contentType, imageBytes);
            user.markProfileCompleted();
            return avatarRepository.save(avatar);
        } catch (java.io.IOException exception) {
            throw new BadRequestException("Could not read the uploaded image.");
        }
    }

    @Transactional(readOnly = true)
    public UserAvatar getAvatar(Long userId) {
        return avatarRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Avatar not found."));
    }

    @Transactional
    public void deleteAvatar(Long userId) {
        avatarRepository.deleteByUserId(userId);
        getUser(userId).markProfileIncomplete();
    }

    public String avatarEtag(UserAvatar avatar) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(avatar.getImageBytes());
            return '"' + java.util.HexFormat.of().formatHex(digest) + '"';
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 should always be available.", exception);
        }
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) return "image/png";
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "image/jpeg";
        String header = new String(bytes, 0, Math.min(bytes.length, 12), StandardCharsets.US_ASCII);
        if (header.startsWith("RIFF") && header.length() >= 12 && header.substring(8, 12).equals("WEBP")) return "image/webp";
        throw new BadRequestException("Avatar must be a PNG, JPEG, or WebP image.");
    }
}
