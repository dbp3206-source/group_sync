package com.groupsync.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.groupsync.backend.auth.dto.UserResponse;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.model.UserAvatar;

class UserResponseTest {
    @Test
    void avatarUrlReflectsAvatarExistenceRatherThanProfileCompletion() {
        UserAccount user = new UserAccount("person@example.com", "encoded-password", "Person");
        ReflectionTestUtils.setField(user, "id", 7L);

        assertThat(user.isProfileCompleted()).isTrue();
        assertThat(UserResponse.from(user).avatarUrl()).isNull();

        UserAvatar avatar = new UserAvatar(user, "image/png", new byte[] { 1, 2, 3 });
        assertThat(UserResponse.from(user, avatar).avatarUrl()).startsWith("/api/users/7/avatar?v=");
    }

    @Test
    void currentUserResponseNeverExposesPasswordHash() {
        UserResponse response = UserResponse.from(new UserAccount("person@example.com", "secret-hash", "Person"));

        assertThat(response.toString()).doesNotContain("secret-hash");
    }
}
