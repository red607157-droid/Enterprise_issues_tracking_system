package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "Issue_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueLabel {

    @EmbeddedId
    private IssueLabelId id;
}