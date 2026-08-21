package com.groupsync.backend.user.controller;

import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.groupsync.backend.auth.dto.UserResponse;
import com.groupsync.backend.auth.security.AuthenticatedUser;
import com.groupsync.backend.user.dto.ChangePasswordRequest;
import com.groupsync.backend.user.dto.UpdateProfileRequest;
import com.groupsync.backend.user.model.UserAvatar;
import com.groupsync.backend.user.service.UserProfileService;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me/profile")
    public UserResponse profile(@AuthenticationPrincipal AuthenticatedUser user) {
        return userProfileService.getProfile(user.getId());
    }

    @PatchMapping("/me/profile")
    public UserResponse updateProfile(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        userProfileService.updateProfile(user.getId(), request);
        return userProfileService.getProfile(user.getId());
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadAvatar(
        @AuthenticationPrincipal AuthenticatedUser user,
        @RequestParam("file") MultipartFile file
    ) {
        userProfileService.saveAvatar(user.getId(), file);
        return userProfileService.getProfile(user.getId());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar(@AuthenticationPrincipal AuthenticatedUser user) {
        userProfileService.deleteAvatar(user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> avatar(@org.springframework.web.bind.annotation.PathVariable Long userId, @org.springframework.web.bind.annotation.RequestHeader(HttpHeaders.IF_NONE_MATCH) Optional<String> ifNoneMatch) {
        UserAvatar avatar = userProfileService.getAvatar(userId);
        String etag = userProfileService.avatarEtag(avatar);
        if (ifNoneMatch.filter(etag::equals).isPresent()) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(avatar.getContentType()))
            .cacheControl(CacheControl.noCache().cachePrivate())
            .eTag(etag)
            .body(avatar.getImageBytes());
    }
}
