package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.dto.BurndownEvent;
import com.example.enterprise_issues_tracking_system.entity.ActivityLog;
import com.example.enterprise_issues_tracking_system.entity.AuditAction;
import com.example.enterprise_issues_tracking_system.entity.AuditEntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ActivityLogRepositoryTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ActivityLogRepository activityLogRepository;

    private void save(Long projectId, Long issueId, AuditEntityType type, AuditAction action, String details) {
        activityLogRepository.saveAndFlush(ActivityLog.builder()
                .entityType(type).entityId(issueId).projectId(projectId).issueId(issueId)
                .action(action).actorId(1L).details(details).build());
    }

    private static final List<AuditAction> BURNDOWN_ACTIONS =
            List.of(AuditAction.CREATED, AuditAction.STATUS_CHANGED, AuditAction.DELETED);

    @Test
    void findBurnDownEvents_shouldKeepOnlyTheProjectIssueEntityAndWantedActions_inOrder() {
        save(1L, 10L, AuditEntityType.ISSUE, AuditAction.CREATED, "t");
        save(1L, 10L, AuditEntityType.ISSUE, AuditAction.STATUS_CHANGED, "OPEN -> IN_PROGRESS");
        save(1L, 10L, AuditEntityType.ISSUE, AuditAction.LABEL_ATTACHED, "label 4");   // wrong action
        save(1L, 11L, AuditEntityType.COMMENT, AuditAction.CREATED, null);              // wrong entity type
        save(2L, 20L, AuditEntityType.ISSUE, AuditAction.CREATED, "other project");     // wrong project

        List<BurndownEvent> events = activityLogRepository.findBurnDownEvents(
                1L, AuditEntityType.ISSUE, BURNDOWN_ACTIONS, LocalDateTime.now().plusDays(1));

        assertEquals(List.of(AuditAction.CREATED, AuditAction.STATUS_CHANGED),
                events.stream().map(BurndownEvent::action).toList());
        assertEquals("OPEN -> IN_PROGRESS", events.get(1).details());
        assertEquals(10L, events.getFirst().issueId());
    }

    @Test
    void findBurnDownEvents_shouldExcludeEventsAtOrAfterTheCutoff() {
        save(1L, 10L, AuditEntityType.ISSUE, AuditAction.CREATED, "t");

        List<BurndownEvent> events = activityLogRepository.findBurnDownEvents(
                1L, AuditEntityType.ISSUE, BURNDOWN_ACTIONS, LocalDateTime.now().minusDays(1));

        assertTrue(events.isEmpty());
    }
}