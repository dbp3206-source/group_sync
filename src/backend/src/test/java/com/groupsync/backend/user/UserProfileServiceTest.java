package com.groupsync.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groupsync.backend.shared.exception.BadRequestException;
import com.groupsync.backend.shared.exception.ForbiddenException;
import com.groupsync.backend.user.dto.ChangePasswordRequest;
import com.groupsync.backend.user.dto.UpdateProfileRequest;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;
import com.groupsync.backend.user.repository.UserAvatarRepository;
import com.groupsync.backend.user.service.UserProfileService;

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
}
