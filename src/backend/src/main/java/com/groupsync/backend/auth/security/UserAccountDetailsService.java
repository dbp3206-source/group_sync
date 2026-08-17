package com.groupsync.backend.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.groupsync.backend.user.repository.UserAccountRepository;

@Service
public class UserAccountDetailsService implements UserDetailsService {
    private final UserAccountRepository userRepository;

    public UserAccountDetailsService(UserAccountRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmail(username.trim().toLowerCase())
            .map(AuthenticatedUser::from)
            .orElseThrow(() -> new UsernameNotFoundException("User not found."));
    }
}
