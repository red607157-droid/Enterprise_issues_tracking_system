package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.CommentRequest;
import com.example.enterprise_issues_tracking_system.dto.CommentResponse;
import com.example.enterprise_issues_tracking_system.dto.CommentUpdateRequest;
import com.example.enterprise_issues_tracking_system.entity.*;
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

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDateTime;
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
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
        verify(projectMemberRepository).findMembership(projectId, callerId);
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

        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> commentService.createComment(request));
        verify(issueRepository).findByIdAndDeletedAtIsNull(issueId);
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
    public void getAllComment_shouldRespond() {
        Long issueId = 1L;
        Issue issue = new Issue();
        issue.setId(issueId);
        Long callerId = 2L;
        User caller = new User();
        caller.setId(callerId);
        Long projectId = 3L;
        Long c1Id = 4L;
        Comment c1 = new Comment();
        c1.setId(c1Id);
        c1.setIssue(issue);
        c1.setBody("this issue must be critical");
        Long c2Id = 5L;
        Comment c2 = new Comment();
        c2.setId(c2Id);
        c2.setIssue(issue);
        c2.setDeletedAt(LocalDateTime.now());
        Long c3Id = 6L;
        Comment c3 = new Comment();
        c3.setId(c3Id);
        c3.setIssue(issue);
        c3.setDeletedAt(LocalDateTime.now());
        Long c4Id = 7L;
        Comment c4 = new Comment();
        c4.setId(c4Id);
        c4.setIssue(issue);
        c4.setBody("John can fix this issue");
        c4.setParentComment(c2);
        List<Comment> allComment = List.of(c1, c2, c3, c4);
        CommentResponse response1 = new CommentResponse();
        response1.setId(c1Id);
        response1.setIssueId(issueId);
        response1.setBody("this issue must be critical");
        CommentResponse response2 = new CommentResponse();
        response2.setId(c2Id);
        response2.setIssueId(issueId);
        CommentResponse response4 = new CommentResponse();
        response4.setId(c4Id);
        response4.setIssueId(issueId);
        response4.setBody("John can fix this issue");
        response4.setParentCommentId(c2Id);

        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
        when(issueRepository.findByIdAndDeletedAtIsNull(issueId)).thenReturn(Optional.of(issue));
        when(projectMemberRepository.findMembership(projectId, callerId)).thenReturn(Optional.of(buildMembership(projectId, callerId, Role.DEVELOPER)));
        when(commentRepository.findAllByIssueId(issueId)).thenReturn(allComment);
        when(commentMapper.toResponse(c1)).thenReturn(response1);
        when(commentMapper.toResponse(c2)).thenReturn(response2);
        when(commentMapper.toResponse(c4)).thenReturn(response4);
        List<CommentResponse> result = commentService.getCommentsByIssue(issueId);

        assertEquals(3, result.size());
        verify(commentMapper, never()).toResponse(c3);
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