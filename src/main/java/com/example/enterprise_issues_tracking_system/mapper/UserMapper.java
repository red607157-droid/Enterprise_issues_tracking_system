package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.RegisterRequest;
import com.example.enterprise_issues_tracking_system.dto.RegisterResponse;
import com.example.enterprise_issues_tracking_system.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(RegisterRequest request, String hashPassword) {
        User user = new User();

        user.setEmail(request.getEmail());
        user.setPassword(hashPassword);
        user.setFullName(request.getFullName());

        return user;
    }

    public RegisterResponse toResponse(User user) {
        RegisterResponse response = new RegisterResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setCreatedAt(user.getCreatedAt());

        return response;
    }
}
