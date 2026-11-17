package com.example.enterprise_issues_tracking_system.repository;

import com.example.enterprise_issues_tracking_system.dto.IssueFilter;
import com.example.enterprise_issues_tracking_system.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
class IssueRepositoryTest {

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
    IssueRepository issueRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired UserRepository userRepository;
    @Autowired LabelRepository labelRepository;
    @Autowired IssueLabelRepository issueLabelRepository;


    private User saveUser(String email, String name) {
        return userRepository.save(User.builder()
                .email(email).password("x").fullName(name)
                .userStatus(UserStatus.ACTIVE).build());
    }

    private Issue saveIssue(Project project, User reporter, User assignee, IssueStatus status) {
        return issueRepository.save(Issue.builder()
                .title("t").type("BUG").dueDate(LocalDateTime.now().plusDays(1))
                .issuePriority(IssuePriority.LOW).issueStatus(status)
                .project(project).reporter(reporter).assignee(assignee).build());
    }

    private Issue saveIssueTitled(Project project, User reporter, String title, IssueStatus status) {
        return issueRepository.save(Issue.builder()
                .title(title).type("BUG").dueDate(LocalDateTime.now().plusDays(1))
                .issuePriority(IssuePriority.LOW).issueStatus(status)
                .project(project).reporter(reporter).build());
    }

    private Label saveLabel(Project project, String name) {
        return labelRepository.save(Label.builder()
                .name(name).color("red").project(project).createdAt(LocalDateTime.now()).build());
    }

    private void attach(Issue issue, Label label) {
        IssueLabelId id = new IssueLabelId();
        id.setIssue(issue);
        id.setLabel(label);
        IssueLabel link = new IssueLabel();
        link.setId(id);
        issueLabelRepository.save(link);
    }

    private Project newProject(User owner) {
        return projectRepository.save(Project.builder().key("ABC").name("Proj").owner(owner).build());
    }

    @Test
    void matching_shouldFilterByStatusAndIgnoreSoftDeletedIssues() {
        User owner = saveUser("o@x.com", "Owner");
        Project project = newProject(owner);
        saveIssueTitled(project, owner, "open one", IssueStatus.OPEN);
        saveIssueTitled(project, owner, "done one", IssueStatus.DONE);
        Issue gone = saveIssueTitled(project, owner, "open deleted", IssueStatus.OPEN);
        gone.softDelete();
        issueRepository.save(gone);

        IssueFilter filter = new IssueFilter(IssueStatus.OPEN, null, null, null, null);
        Page<Issue> page = issueRepository.findAll(IssueSpecifications.matching(project.getId(), filter), Pageable.unpaged());

        assertEquals(List.of("open one"), page.getContent().stream().map(Issue::getTitle).toList());
    }

    @Test
    void matching_shouldTreatPercentInSearchTextAsLiteral() {
        User owner = saveUser("o@x.com", "Owner");
        Project project = newProject(owner);
        saveIssueTitled(project, owner, "50% off", IssueStatus.OPEN);
        saveIssueTitled(project, owner, "50 off", IssueStatus.OPEN);

        IssueFilter filter = new IssueFilter(null, null, null, null, "50%");
        Page<Issue> page = issueRepository.findAll(IssueSpecifications.matching(project.getId(), filter), Pageable.unpaged());

        assertEquals(List.of("50% off"), page.getContent().stream().map(Issue::getTitle).toList());
    }

    @Test
    void matching_byLabel_shouldReturnLinkedIssuesAndIgnoreSoftDeletedLabels() {
        User owner = saveUser("o@x.com", "Owner");
        Project project = newProject(owner);
        Issue tagged = saveIssueTitled(project, owner, "tagged", IssueStatus.OPEN);
        saveIssueTitled(project, owner, "plain", IssueStatus.OPEN);
        Label live = saveLabel(project, "bug");
        Label dead = saveLabel(project, "old");
        attach(tagged, live);
        attach(tagged, dead);
        dead.softDelete();
        labelRepository.save(dead);

        Page<Issue> byLive = issueRepository.findAll(IssueSpecifications.matching(project.getId(),
                new IssueFilter(null, null, null, live.getId(), null)), Pageable.unpaged());
        Page<Issue> byDead = issueRepository.findAll(IssueSpecifications.matching(project.getId(),
                new IssueFilter(null, null, null, dead.getId(), null)), Pageable.unpaged());

        assertEquals(List.of("tagged"), byLive.getContent().stream().map(Issue::getTitle).toList());
        assertTrue(byDead.isEmpty());
    }

    @Test
    void countByAssignee_shouldCountTotalAndDonePerAssignee() {
        User owner = saveUser("o@x.com", "Owner");
        User ana = saveUser("a@x.com", "Ana");
        Project project = projectRepository.save(Project.builder()
                .key("ABC").name("Proj").owner(owner).build());
        saveIssue(project, owner, ana, IssueStatus.OPEN);
        saveIssue(project, owner, ana, IssueStatus.DONE);
        saveIssue(project, owner, null, IssueStatus.OPEN);

        List<Object[]> rows = issueRepository.countByAssignee(project.getId(), IssueStatus.DONE);

        assertEquals(2, rows.size());
        Object[] anaRow = rows.stream().filter(r -> r[0] != null).findFirst().orElseThrow();
        assertEquals("Ana", anaRow[1]);
        assertEquals(2L, ((Number) anaRow[2]).longValue());
        assertEquals(1L, ((Number) anaRow[3]).longValue());
    }

    @Test
    void countByStatus_shouldIgnoreSoftDeletedIssues() {
        User owner = saveUser("o@x.com", "Owner");
        Project project = projectRepository.save(Project.builder()
                .key("ABC").name("Proj").owner(owner).build());
        saveIssue(project, owner, null, IssueStatus.OPEN);
        Issue deleted = saveIssue(project, owner, null, IssueStatus.OPEN);
        deleted.softDelete();
        issueRepository.save(deleted);

        List<Object[]> rows = issueRepository.countByStatus(project.getId());

        assertEquals(1, rows.size());
        assertEquals(1L, rows.getFirst()[1]);
    }
}