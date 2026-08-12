package com.groupsync.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.groupsync.backend.auth.dto.RegisterRequest;
import com.groupsync.backend.auth.service.AuthService;
import com.groupsync.backend.shared.exception.ConflictException;
import com.groupsync.backend.user.model.UserAccount;
import com.groupsync.backend.user.repository.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void registerNormalizesEmailAndStoresEncodedPassword() {
        when(userRepository.existsByEmail("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount user = new AuthService(userRepository, passwordEncoder)
            .register(new RegisterRequest(" Person@Example.com ", "secret123", "Person"));

        assertThat(user.getEmail()).isEqualTo("person@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$encoded");
        assertThat(user.getPasswordHash()).isNotEqualTo("secret123");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> new AuthService(userRepository, passwordEncoder)
            .register(new RegisterRequest("person@example.com", "secret123", "Person")))
            .isInstanceOf(ConflictException.class);
    }
}
