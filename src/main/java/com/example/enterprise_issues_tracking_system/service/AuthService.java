package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.*;
import com.example.enterprise_issues_tracking_system.entity.RefreshToken;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.entity.UserStatus;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.InvalidCredentialsException;
import com.example.enterprise_issues_tracking_system.mapper.UserMapper;
import com.example.enterprise_issues_tracking_system.repository.RefreshTokenRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import com.example.enterprise_issues_tracking_system.security.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email has already been registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .userStatus(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
        checkAccountIsActive(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        RefreshToken refresh = RefreshToken.builder()
                .refreshToken(refreshToken)
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .user(user)
                .build();
        refreshTokenRepository.save(refresh);
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        return response;
    }

    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findById(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));
        if (LocalDateTime.now().isAfter(refreshToken.getExpiredAt())) {
            throw new InvalidCredentialsException("Your refresh token has expired");
        }
        User user = refreshToken.getUser();
        checkAccountIsActive(user);

        String accessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.deleteById(refreshToken.getRefreshToken());
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .refreshToken(newRefreshToken)
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .user(user)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);
        RefreshResponse response = new RefreshResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }

    public void logout(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findById(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("refresh token has already been logged out"));
        refreshTokenRepository.deleteById(refreshToken.getRefreshToken());
    }

    private void checkAccountIsActive(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Account is not active");
        }
    }
}