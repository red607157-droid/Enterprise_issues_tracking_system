package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.dto.IssueFilter;
import com.example.enterprise_issues_tracking_system.entity.Issue;
import com.example.enterprise_issues_tracking_system.entity.IssueLabel;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> matching (Long projectId, IssueFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("issueStatus"), filter.status()));
            }
            if (filter.assigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), filter.assigneeId()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("issuePriority"), filter.priority()));
            }
            if (filter.labelId() != null) {
                Subquery<Long> linked = query.subquery(Long.class);
                Root<IssueLabel> link = linked.from(IssueLabel.class);
                linked.select(link.get("id").get("issue"))
                        .where(
                                cb.equal(link.get("id").get("issue").get("id"), root.get("id")),
                                cb.equal(link.get("id").get("label").get("id"), filter.labelId()),
                                cb.isNull(link.get("id").get("label").get("deletedAt"))
                        );
                predicates.add(cb.exists(linked));
            }
            if (filter.q() != null && !filter.q().isBlank()) {
                String pattern = "%" + escapeLike(filter.q().trim().toLowerCase()) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("description"), "")), pattern, '\\')
                        ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}