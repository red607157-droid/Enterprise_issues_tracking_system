package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.LoginRequest;
import com.example.enterprise_issues_tracking_system.dto.LoginResponse;
import com.example.enterprise_issues_tracking_system.dto.RegisterRequest;
import com.example.enterprise_issues_tracking_system.dto.RegisterResponse;
import com.example.enterprise_issues_tracking_system.entity.RefreshToken;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.mapper.UserMapper;
import com.example.enterprise_issues_tracking_system.repository.RefreshTokenRepository;
import com.example.enterprise_issues_tracking_system.repository.UserRepository;
import com.example.enterprise_issues_tracking_system.security.JWTUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    public void register_whenEmailIsUnique_shouldCreateUser() {
        String email = "end123@gmail.com";
        RegisterResponse expectedResponse = new RegisterResponse();
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userMapper.toResponse(any(User.class))).thenReturn(expectedResponse);

        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFullName("Kelvin");
        RegisterResponse result = authService.register(request);

        assertEquals(expectedResponse, result);
        verify(userRepository).save(any(User.class));

        ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userArgumentCaptor.capture());
        assertEquals("hashedPassword123", userArgumentCaptor.getValue().getPassword());
    }

    @Test
    public void register_whenEmailAlreadyExists_shouldThrowException() {
        String email = "end123@gmail.com";
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("password123");
        request.setFullName("Kelvin");
        when(userRepository.existsByEmail(email)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    public void login_whenEmailAlreadyExists_shouldCreateToken() {
        String email = "end123@gmail.com";
        User user = new User();
        user.setPassword("hashedPassword123");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword123")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user)).thenReturn("fake-access-token");

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");
        LoginResponse result = authService.login(request);

        assertEquals("fake-access-token", result.getAccessToken());
        assertNotNull(result.getRefreshToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    public void login_whenEmailNotExists_shouldThrowException() {
        String email = "end123@gmail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    public void login_whenPasswordWrong_shouldThrowException() {
        String email = "end123@gmail.com";
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashedPassword123")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> authService.login(request));
        verify(jwtUtil, never()).generateAccessToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

}