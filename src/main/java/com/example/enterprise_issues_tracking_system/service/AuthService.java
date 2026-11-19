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
import java.util.Locale;
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

    private String dummyHash;

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = passwordEncoder.encode("not-a-real-password");
        }
        return dummyHash;
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalize(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email has already been registered");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .userStatus(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
        return userMapper.toResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalize(request.getEmail()))
                .orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.getPassword(), dummyHash());
            throw new InvalidCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
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

    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findById(request.getRefreshToken())
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token not found"));
        if (LocalDateTime.now().isAfter(refreshToken.getExpiredAt())) {
            refreshTokenRepository.deleteByToken(refreshToken.getRefreshToken());
            throw new InvalidCredentialsException("Your refresh token has expired");
        }
        User user = refreshToken.getUser();
        checkAccountIsActive(user);
        if (refreshTokenRepository.deleteByToken(refreshToken.getRefreshToken()) != 1) {
            throw new InvalidCredentialsException("Refresh token not found");
        }
        String accessToken = jwtUtil.generateAccessToken(user);
        String newRefreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .refreshToken(newRefreshToken)
                .expiredAt(LocalDateTime.now().plusMonths(1))
                .user(user)
                .build());
        RefreshResponse response = new RefreshResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(newRefreshToken);
        return response;
    }

    public void logout(RefreshRequest request) {
        refreshTokenRepository.deleteByToken(request.getRefreshToken());
    }

    private void checkAccountIsActive(User user) {
        if (user.getUserStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Account is not active");
        }
    }
}