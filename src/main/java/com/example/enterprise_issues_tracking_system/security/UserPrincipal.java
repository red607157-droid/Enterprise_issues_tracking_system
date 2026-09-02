package com.example.enterprise_issues_tracking_system.security;

import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

    private final User user;

    public String getUsername() {
        return user.getEmail();
    }

    public String getPassword() {
        return user.getPassword();
    }

    public User getUser() {
        return user;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    public boolean isAccountNonLocked() {
        return !(user.getUserStatus().equals(UserStatus.SUSPENDED));
    }

    public boolean isEnabled() {
        return user.getUserStatus().equals(UserStatus.ACTIVE);
    }

    public boolean isAccountNonExpired() {
        return true;
    }

    public boolean isCredentialsNonExpired() {
        return true;
    }

}
