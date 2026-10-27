package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
public class IssueLabelId implements Serializable {

    @ManyToOne
    @JoinColumn(name = "issues_id")
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "labels_id")
    private Label label;
}