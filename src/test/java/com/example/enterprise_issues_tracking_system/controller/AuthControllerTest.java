package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.RefreshRequest;
import com.example.enterprise_issues_tracking_system.dto.RegisterRequest;
import com.example.enterprise_issues_tracking_system.dto.RegisterResponse;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.InvalidCredentialsException;
import com.example.enterprise_issues_tracking_system.security.JWTFilter;
import com.example.enterprise_issues_tracking_system.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JWTFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AuthService authService;

    @Test
    void register_whenValid_shouldReturn201() throws Exception {
        RegisterResponse response = new RegisterResponse();
        response.setId(1L);
        response.setEmail("a@x.com");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@x.com","password":"secret123","fullName":"Ana"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@x.com"));
    }

    @Test
    void register_whenEmailInvalidAndPasswordShort_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"123","fullName":"Ana"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(authService);
    }

    @Test
    void register_whenEmailTaken_shouldReturn409() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateResourceException("Email has already been registered"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@x.com","password":"secret123","fullName":"Ana"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email has already been registered"));
    }

    @Test
    void login_whenCredentialsWrong_shouldReturn401() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@x.com","password":"wrong"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_whenBodyIsMalformed_shouldReturn400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_whenValid_shouldReturn204() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"abc"}"""))
                .andExpect(status().isNoContent());
        verify(authService).logout(any(RefreshRequest.class));
    }
}