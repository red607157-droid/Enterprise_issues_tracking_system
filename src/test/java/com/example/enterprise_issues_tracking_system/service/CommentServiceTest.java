package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.CommentRequest;
import com.example.enterprise_issues_tracking_system.dto.CommentResponse;
import com.example.enterprise_issues_tracking_system.dto.CommentUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.CommentMapper;
import com.example.enterprise_issues_tracking_system.repository.CommentRepository;
import com.example.enterprise_issues_tracking_system.repository.IssueRepository;
import com.example.enterprise_issues_tracking_system.repository.ProjectMemberRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CommentService commentService;

    private ProjectMember buildMembership(Long projectId, Long userId, Role role) {
        ProjectMemberId id = new ProjectMemberId();
        Project project = new Project();
        project.setId(projectId);
        User user = new User();
        user.setId(userId);
        id.setProject(project);
        id.setUser(user);
        ProjectMember member = new ProjectMember();
        member.setId(id);
        member.setRole(role);
        return member;
    }

    @Test
    public void createComment_whenAuthorized_shouldCreate() {
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        String body = "this issue can be fixed quickly";
        CommentRequest request = new CommentRequest();
        request.setIssueId(issueId);
        request.setBody(body);
        Comment comment = new Comment();
        comment.setIssue(issue);
        comment.setBody(body);
        CommentResponse response = new CommentResponse();
        response.setIssueId(issueId);
        response.setBody(body);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(commentMapper.toEntity(request)).thenReturn(comment);
        when(commentMapper.toResponse(comment)).thenReturn(response);
        CommentResponse result = commentService.createComment(request);

        assertEquals(response, result);
        verify(commentRepository).save(any(Comment.class));

        ArgumentCaptor<Comment> argumentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(argumentCaptor.capture());
        assertNotEquals(null, argumentCaptor.getValue().getAuthor());

    }

    @Test
    public void createComment_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        CommentRequest request = new CommentRequest();
        request.setIssueId(issueId);

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> commentService.createComment(request));
        verify(commentRepository, never()).save(any());
    }

    @Test
    public void getComment_whenFound_shouldRespond() {
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setIssue(issue);
        CommentResponse response = new CommentResponse();
        response.setId(commentId);
        response.setIssueId(issueId);

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(commentMapper.toResponse(comment)).thenReturn(response);
        CommentResponse result = commentService.getCommentById(commentId);

        assertEquals(response, result);
        verify(commentRepository).findByIdAndDeletedAtIsNull(commentId);
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
    }

    @Test
    public void getComment_whenNotFound_shouldThrowException() {
        Long commentId = 1L;

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> commentService.getCommentById(commentId));
        verify(commentRepository).findByIdAndDeletedAtIsNull(commentId);
    }

    @Test
    public void getComment_whenUnauthorized_shouldThrowException() {
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 4L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setIssue(issue);

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> commentService.getCommentById(commentId));
        verify(commentRepository).findByIdAndDeletedAtIsNull(commentId);
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
    }

    @Test
    public void getAllComment_whenMember_shouldRespondWithMappedPage() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Comment c1 = new Comment();
        c1.setId(4L);
        Comment c2 = new Comment();
        c2.setId(5L);
        CommentResponse response1 = new CommentResponse();
        response1.setId(4L);
        CommentResponse response2 = new CommentResponse();
        response2.setId(5L);
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(commentRepository.findVisibleByIssueId(issueId, pageable)).thenReturn(new PageImpl<>(List.of(c1, c2)));
        when(commentMapper.toResponse(c1)).thenReturn(response1);
        when(commentMapper.toResponse(c2)).thenReturn(response2);

        Page<CommentResponse> result = commentService.getCommentsByIssue(issueId, pageable);

        assertEquals(List.of(response1, response2), result.getContent());
    }

    @Test
    public void getAllComment_whenNotMember_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> commentService.getCommentsByIssue(issueId, PageRequest.of(0, 10)));
        verify(commentRepository, never()).findVisibleByIssueId(any(), any());
    }

    @Test
    public void getAllComment_whenSortFieldNotAllowed_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        issue.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));

        assertThrows(InvalidRequestException.class,
                () -> commentService.getCommentsByIssue(issueId, PageRequest.of(0, 10, Sort.by("body"))));
        verify(commentRepository, never()).findVisibleByIssueId(any(), any());
    }

    @Test
    public void updateComment_whenAuthorized_shouldUpdate() {
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        String body = "John can fix this issue";
        Long existingCommentId = 1L;
        Comment existingComment = new Comment();
        existingComment.setId(existingCommentId);
        existingComment.setBody("this issue can be fixed quickly");
        existingComment.setAuthor(caller);
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setBody(body);
        Comment newComment = new Comment();
        newComment.setId(existingCommentId);
        newComment.setBody(body);
        newComment.setAuthor(caller);
        CommentResponse response = new CommentResponse();
        response.setId(existingCommentId);
        response.setBody(body);
        response.setAuthorId(callerId);

        when(commentRepository.findByIdAndDeletedAtIsNull(existingCommentId)).thenReturn(Optional.of(existingComment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(commentMapper.updateEntity(request, existingComment)).thenReturn(newComment);
        when(commentMapper.toResponse(newComment)).thenReturn(response);
        CommentResponse result = commentService.updateComment(existingCommentId, request);

        assertEquals(response, result);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    public void updateComment_whenUnauthorized_shouldThrowException() {
        Long authorId = 3L;
        User author = new User();
        author.setId(authorId);
        Long existingCommentId = 1L;
        Comment existingComment = new Comment();
        existingComment.setId(existingCommentId);
        existingComment.setAuthor(author);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        CommentUpdateRequest request = new CommentUpdateRequest();

        when(commentRepository.findByIdAndDeletedAtIsNull(existingCommentId)).thenReturn(Optional.of(existingComment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);

        assertThrows(AccessDeniedException.class, () -> commentService.updateComment(existingCommentId, request));
        verify(commentRepository, never()).save(any());
    }

    @Test
    public void deleteComment_whenCallerIsAdmin_shouldDelete() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Issue issue = new Issue();
        issue.setId(5L);
        issue.setProject(project);
        Long authorId = 4L;
        User author = new User();
        author.setId(authorId);
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(author);
        comment.setIssue(issue);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.ADMIN)));
        commentService.deleteComment(commentId);

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    public void deleteComment_whenCallerIsAuthor_shouldDelete() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Issue issue = new Issue();
        issue.setId(5L);
        issue.setProject(project);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(caller);
        comment.setIssue(issue);

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        commentService.deleteComment(commentId);

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    public void deleteComment_whenUnauthorized_shouldThrowException() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        Issue issue = new Issue();
        issue.setId(5L);
        issue.setProject(project);
        Long authorId = 4L;
        User author = new User();
        author.setId(authorId);
        Long commentId = 1L;
        Comment comment = new Comment();
        comment.setId(commentId);
        comment.setAuthor(author);
        comment.setIssue(issue);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);

        when(commentRepository.findByIdAndDeletedAtIsNull(commentId)).thenReturn(Optional.of(comment));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository, never()).save(any());
    }
}