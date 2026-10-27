package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.LabelResponse;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.DuplicateResourceException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.LabelMapper;
import com.example.enterprise_issues_tracking_system.repository.IssueLabelRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.LabelRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueLabelServiceTest {

    private static final Long ISSUE_ID = 1L;
    private static final Long CALLER_ID = 2L;
    private static final Long PROJECT_ID = 3L;
    private static final Long LABEL_ID = 4L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private IssueLabelRepository issueLabelRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private LabelMapper labelMapper;

    @InjectMocks
    private IssueLabelService issueLabelService;

    private Project project;
    private User caller;
    private Issue issue;
    private Label label;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(PROJECT_ID);
        caller = new User();
        caller.setId(CALLER_ID);
        issue = new Issue();
        issue.setId(ISSUE_ID);
        issue.setProject(project);
        label = new Label();
        label.setId(LABEL_ID);
        label.setProject(project);
    }

    private ProjectMember buildMembership(Role role) {
        ProjectMemberId id = new ProjectMemberId();
        id.setProject(project);
        id.setUser(caller);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    private void givenIssueAndProject() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.of(issue));
        when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));
    }

    private void givenCallerRole(Role role) {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.of(buildMembership(role)));
    }

    @Test
    public void addLabel_whenCallerIsManager_shouldCreateLink() {
        givenIssueAndProject();
        givenCallerRole(Role.PROJECT_MANAGER);
        when(labelRepository.findByIdAndDeletedAtIsNull(LABEL_ID)).thenReturn(Optional.of(label));
        when(issueLabelRepository.findLink(ISSUE_ID, LABEL_ID)).thenReturn(Optional.empty());

        issueLabelService.addLabel(ISSUE_ID, LABEL_ID);

        ArgumentCaptor<IssueLabel> captor = ArgumentCaptor.forClass(IssueLabel.class);
        verify(issueLabelRepository).save(captor.capture());
        assertEquals(issue, captor.getValue().getId().getIssue());
        assertEquals(label, captor.getValue().getId().getLabel());
    }

    @Test
    public void addLabel_whenCallerIsAssignee_shouldCreateLink() {
        issue.setAssignee(caller);
        givenIssueAndProject();
        givenCallerRole(Role.DEVELOPER);
        when(labelRepository.findByIdAndDeletedAtIsNull(LABEL_ID)).thenReturn(Optional.of(label));
        when(issueLabelRepository.findLink(ISSUE_ID, LABEL_ID)).thenReturn(Optional.empty());

        issueLabelService.addLabel(ISSUE_ID, LABEL_ID);

        verify(issueLabelRepository).save(any(IssueLabel.class));
    }

    @Test
    public void addLabel_whenDeveloperIsNotAssignee_shouldThrowException() {
        givenIssueAndProject();
        givenCallerRole(Role.DEVELOPER);

        assertThrows(AccessDeniedException.class, () -> issueLabelService.addLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).save(any());
    }

    @Test
    public void addLabel_whenLabelBelongsToAnotherProject_shouldThrowException() {
        Project otherProject = new Project();
        otherProject.setId(99L);
        label.setProject(otherProject);
        givenIssueAndProject();
        givenCallerRole(Role.ADMIN);
        when(labelRepository.findByIdAndDeletedAtIsNull(LABEL_ID)).thenReturn(Optional.of(label));

        assertThrows(ResourceNotFoundException.class, () -> issueLabelService.addLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).save(any());
    }

    @Test
    public void addLabel_whenAlreadyAttached_shouldThrowException() {
        givenIssueAndProject();
        givenCallerRole(Role.ADMIN);
        when(labelRepository.findByIdAndDeletedAtIsNull(LABEL_ID)).thenReturn(Optional.of(label));
        when(issueLabelRepository.findLink(ISSUE_ID, LABEL_ID)).thenReturn(Optional.of(new IssueLabel()));

        assertThrows(DuplicateResourceException.class, () -> issueLabelService.addLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).save(any());
    }

    @Test
    public void addLabel_whenIssueNotFound_shouldThrowException() {
        when(issueRepository.findByIdAndDeletedAtIsNull(ISSUE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueLabelService.addLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).save(any());
    }

    @Test
    public void removeLabel_whenAuthorized_shouldDeleteLink() {
        IssueLabel link = new IssueLabel();
        givenIssueAndProject();
        givenCallerRole(Role.ADMIN);
        when(issueLabelRepository.findLink(ISSUE_ID, LABEL_ID)).thenReturn(Optional.of(link));

        issueLabelService.removeLabel(ISSUE_ID, LABEL_ID);

        verify(issueLabelRepository).delete(link);
    }

    @Test
    public void removeLabel_whenNotAttached_shouldThrowException() {
        givenIssueAndProject();
        givenCallerRole(Role.ADMIN);
        when(issueLabelRepository.findLink(ISSUE_ID, LABEL_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> issueLabelService.removeLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).delete(any());
    }

    @Test
    public void removeLabel_whenDeveloperIsNotAssignee_shouldThrowException() {
        givenIssueAndProject();
        givenCallerRole(Role.DEVELOPER);

        assertThrows(AccessDeniedException.class, () -> issueLabelService.removeLabel(ISSUE_ID, LABEL_ID));
        verify(issueLabelRepository, never()).delete(any());
    }

    @Test
    public void getLabelsByIssue_whenMember_shouldRespond() {
        Label second = new Label();
        second.setId(5L);
        second.setProject(project);
        LabelResponse firstResponse = new LabelResponse();
        firstResponse.setId(LABEL_ID);
        LabelResponse secondResponse = new LabelResponse();
        secondResponse.setId(5L);
        givenIssueAndProject();
        givenCallerRole(Role.DEVELOPER);
        when(issueLabelRepository.findActiveLabelsByIssueId(ISSUE_ID)).thenReturn(List.of(label, second));
        when(labelMapper.toResponse(label)).thenReturn(firstResponse);
        when(labelMapper.toResponse(second)).thenReturn(secondResponse);

        List<LabelResponse> result = issueLabelService.getLabelsByIssue(ISSUE_ID);

        assertEquals(List.of(firstResponse, secondResponse), result);
    }

    @Test
    public void getLabelsByIssue_whenNotMember_shouldThrowException() {
        givenIssueAndProject();
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(PROJECT_ID, CALLER_ID)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> issueLabelService.getLabelsByIssue(ISSUE_ID));
        verify(issueLabelRepository, never()).findActiveLabelsByIssueId(any());
    }
}