package com.groupsync.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.groupsync.backend.auth.dto.UserResponse;
import com.groupsync.backend.user.model.UserAccount;

class UserResponseTest {
    @Test
    void avatarUrlIsOnlyExposedAfterProfileCompletion() {
        UserAccount user = new UserAccount("person@example.com", "encoded-password", "Person");

        assertThat(UserResponse.from(user).avatarUrl()).isNull();

        user.markProfileCompleted();
        assertThat(UserResponse.from(user).avatarUrl()).startsWith("/api/users/").endsWith("/avatar");
    }
}
