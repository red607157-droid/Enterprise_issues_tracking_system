package com.example.enterprise_issues_tracking_system.security;

import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<User> user = userRepository.findByEmail(email);
        return new UserPrincipal(user.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email)));

    }
}
