package com.example.enterprise_issues_tracking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RefreshToken {

    @Id
    @EqualsAndHashCode.Include
    private String refreshToken;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
