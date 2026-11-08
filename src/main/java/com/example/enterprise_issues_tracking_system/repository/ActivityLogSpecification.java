package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ActivityLogSpecification {

    private ActivityLogSpecification() {
    }
    public static Specification<ActivityLog> forProject(Long projectId, AuditEntityType entityType, Long actorId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (actorId != null) {
                predicates.add(cb.equal(root.get("actorId"), actorId));
            }
            return cb.and(predicates.toArray(predicates.toArray(new Predicate[0])));
        };
    }
}
