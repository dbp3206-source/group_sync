package com.groupsync.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.shared.exception.NotFoundException;
import com.groupsync.backend.auth.dto.UserResponse;
import com.groupsync.backend.user.dto.ChangePasswordRequest;
import com.groupsync.backend.user.dto.UpdateProfileRequest;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;
import com.groupsync.backend.user.repository.UserAvatarRepository;
import com.groupsync.backend.user.service.UserProfileService;
import com.groupsync.backend.user.model.UserAvatar;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {
    @Mock private UserAccountRepository userRepository;
    @Mock private UserAvatarRepository avatarRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Test
    void updateProfileStoresDisplayNameAndRecognizedTimeZone() {
        UserAccount user = new UserAccount("person@example.com", "hash", "Old name");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        new UserProfileService(userRepository, avatarRepository, passwordEncoder)
            .updateProfile(7L, new UpdateProfileRequest("New name", "Asia/Ho_Chi_Minh"));

        assertThat(user.getDisplayName()).isEqualTo("New name");
        assertThat(user.getTimeZone()).isEqualTo("Asia/Ho_Chi_Minh");
        assertThat(user.isProfileCompleted()).isTrue();
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        UserAccount user = new UserAccount("person@example.com", "hash", "Person");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> new UserProfileService(userRepository, avatarRepository, passwordEncoder)
            .changePassword(7L, new ChangePasswordRequest("wrong", "new-password")))
            .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void avatarRejectsFilesThatAreNotImages() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "not an image".getBytes());

        assertThatThrownBy(() -> new UserProfileService(userRepository, avatarRepository, passwordEncoder).saveAvatar(7L, file))
            .isInstanceOf(BadRequestException.class);

        verify(avatarRepository, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void profileCompletionDependsOnDisplayNameNotAvatarOrTimeZone() {
        UserAccount user = new UserAccount("person@example.com", "hash", "Person");
        user.updateProfile("Person", null);
        assertThat(user.isProfileCompleted()).isTrue();
        user.updateProfile("  ", "Asia/Ho_Chi_Minh");
        assertThat(user.isProfileCompleted()).isFalse();
    }

    @Test
    void currentProfileOnlyIncludesAvatarUrlWhenAvatarExists() {
        UserAccount user = userWithId(7L);
        UserAvatar avatar = new UserAvatar(user, "image/png", pngBytes());
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(avatarRepository.findByUserId(7L)).thenReturn(Optional.of(avatar));
        UserResponse response = service().getProfile(7L);
        assertThat(response.profileCompleted()).isTrue();
        assertThat(response.avatarUrl()).startsWith("/api/users/7/avatar?v=");
    }

    @Test
    void currentProfileUsesInitialsFallbackWhenAvatarIsAbsent() {
        UserAccount user = userWithId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(avatarRepository.findByUserId(7L)).thenReturn(Optional.empty());
        UserResponse response = service().getProfile(7L);
        assertThat(response.profileCompleted()).isTrue();
        assertThat(response.avatarUrl()).isNull();
    }

    @Test
    void avatarUploadCreatesAvatarForAuthenticatedOwnerWithoutChangingCompletion() {
        UserAccount user = userWithId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(avatarRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(avatarRepository.save(any(UserAvatar.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserAvatar saved = service().saveAvatar(7L, avatarFile(pngBytes()));
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getImageBytes()).isEqualTo(pngBytes());
        assertThat(user.isProfileCompleted()).isTrue();
    }

    @Test
    void avatarUploadReplacesExistingOwnerAvatar() {
        UserAccount user = userWithId(7L);
        UserAvatar existing = new UserAvatar(user, "image/png", pngBytes());
        byte[] replacement = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1 };
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(avatarRepository.findByUserId(7L)).thenReturn(Optional.of(existing));
        when(avatarRepository.save(existing)).thenReturn(existing);
        UserAvatar saved = service().saveAvatar(7L, avatarFile(replacement));
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getImageBytes()).isEqualTo(replacement);
    }

    @Test
    void avatarDeleteKeepsCompletedProfileAndOnlyDeletesAuthenticatedOwnerAvatar() {
        UserAccount user = userWithId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        service().deleteAvatar(7L);
        verify(avatarRepository).deleteByUserId(7L);
        assertThat(user.isProfileCompleted()).isTrue();
    }

    @Test
    void unknownOwnerCannotCreateAvatar() {
        when(userRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service().saveAvatar(8L, avatarFile(pngBytes())))
            .isInstanceOf(NotFoundException.class);
        verify(avatarRepository, never()).save(any());
    }

    @Test
    void changePasswordEncodesNewPasswordAfterCurrentPasswordMatches() {
        UserAccount user = userWithId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
        service().changePassword(7L, new ChangePasswordRequest("current-password", "new-password"));
        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    }

    private UserProfileService service() {
        return new UserProfileService(userRepository, avatarRepository, passwordEncoder);
    }

    private UserAccount userWithId(Long id) {
        UserAccount user = new UserAccount("person@example.com", "hash", "Person");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private MockMultipartFile avatarFile(byte[] bytes) {
        return new MockMultipartFile("file", "avatar", "application/octet-stream", bytes);
    }

    private byte[] pngBytes() {
        return new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1 };
    }
}
