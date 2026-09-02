package com.example.enterprise_issues_tracking_system.security;

import com.example.enterprise_issues_tracking_system.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public User getCurrentUser() {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userPrincipal.getUser();
    }
}
