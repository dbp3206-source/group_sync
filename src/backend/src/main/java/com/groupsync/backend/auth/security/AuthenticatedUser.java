package com.groupsync.backend.auth.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.groupsync.backend.user.model.UserAccount;

public class AuthenticatedUser implements UserDetails {
    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final Collection<? extends GrantedAuthority> authorities;

    private AuthenticatedUser(UserAccount user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.displayName = user.getDisplayName();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getSystemRole().name()));
    }

    public static AuthenticatedUser from(UserAccount user) {
        return new AuthenticatedUser(user);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
