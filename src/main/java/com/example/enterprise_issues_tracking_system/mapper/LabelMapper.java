package com.example.enterprise_issues_tracking_system.mapper;

import com.example.enterprise_issues_tracking_system.dto.LabelRequest;
import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.dto.LabelUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.Label;
import com.example.enterprise_issues_tracking_system.entity.Project;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class LabelMapper {

    public Label toEntity(LabelRequest request) {

        Project project = new Project();
        project.setId(request.getProjectId());

        Label label = new Label();
        label.setProject(project);
        label.setName(request.getName());
        label.setColor(request.getColor());
        label.setCreatedAt(LocalDateTime.now());

        return label;
    }

    public LabelResponse toResponse(Label label) {

        LabelResponse response = new LabelResponse();
        response.setId(label.getId());
        response.setColor(label.getColor());
        response.setCreatedAt(label.getCreatedAt());
        response.setName(label.getName());
        response.setProjectId(label.getProject().getId());

        return response;
    }

    public Label updateEntity(LabelUpdateRequest request, Label existingLabel) {

        existingLabel.setColor(request.getColor());
        existingLabel.setName(request.getName());

        return existingLabel;
    }
}
