package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findByIdAndDeletedAtIsNull(Long id);
    List<Label> findAllByProjectIdAndDeletedAtIsNull(Long projectId);
}