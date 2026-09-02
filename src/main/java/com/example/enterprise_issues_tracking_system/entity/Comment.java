package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Builder
public class Comment {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    public void softDelete() {
        deletedAt = LocalDateTime.now();
    }
}
