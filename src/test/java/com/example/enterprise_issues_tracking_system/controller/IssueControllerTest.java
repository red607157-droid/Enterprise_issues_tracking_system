package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.dto.IssueFilter;
import com.example.enterprise_issues_tracking_system.dto.IssueRequest;
import com.example.enterprise_issues_tracking_system.dto.IssueResponse;
import com.example.enterprise_issues_tracking_system.entity.IssuePriority;
import com.example.enterprise_issues_tracking_system.entity.IssueStatus;
import com.example.enterprise_issues_tracking_system.exception.InvalidStatusTransitionException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.security.JWTFilter;
import com.example.enterprise_issues_tracking_system.service.IssueService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = IssueController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JWTFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class IssueControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    IssueService issueService;

    @Test
    void getIssuesByProject_whenStatusIsBogus_shouldReturn400() throws Exception {
        mockMvc.perform(get("/projects/1/issues").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("status")));
        verifyNoInteractions(issueService);
    }

    @Test
    void getIssuesByProject_shouldPassFilterAndPageableToTheService() throws Exception {
        when(issueService.getIssuesByProject(eq(1L), any(IssueFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/projects/1/issues")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("assigneeId", "7")
                        .param("q", "login")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<IssueFilter> filterCaptor = ArgumentCaptor.forClass(IssueFilter.class);
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(issueService).getIssuesByProject(eq(1L), filterCaptor.capture(), pageCaptor.capture());
        assertEquals(IssueStatus.OPEN, filterCaptor.getValue().status());
        assertEquals(IssuePriority.HIGH, filterCaptor.getValue().priority());
        assertEquals(7L, filterCaptor.getValue().assigneeId());
        assertEquals("login", filterCaptor.getValue().q());
        assertEquals(2, pageCaptor.getValue().getPageNumber());
        assertEquals(5, pageCaptor.getValue().getPageSize());
    }

    @Test
    void getIssuesByProject_withoutParams_shouldUseDefaultPageSizeAndSort() throws Exception {
        when(issueService.getIssuesByProject(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/projects/1/issues")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(issueService).getIssuesByProject(eq(1L), any(), pageCaptor.capture());
        assertEquals(20, pageCaptor.getValue().getPageSize());
        assertEquals(Sort.Direction.DESC, pageCaptor.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void createIssue_whenRequiredFieldsMissing_shouldReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(greaterThan(1)));
        verifyNoInteractions(issueService);
    }

    @Test
    void createIssue_whenValid_shouldReturn201() throws Exception {
        IssueResponse response = new IssueResponse();
        response.setId(9L);
        when(issueService.createIssue(any(IssueRequest.class))).thenReturn(response);

        mockMvc.perform(post("/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":1,"title":"Login bug","type":"BUG",
                                 "issuePriority":"HIGH","dueDate":"2026-12-01T10:00:00"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void updateIssue_whenStatusIsInvalid_shouldReturn400() throws Exception {
        when(issueService.updateIssue(eq(1L), any()))
                .thenThrow(new InvalidStatusTransitionException("Cannot move an issue from OPEN to DONE"));

        mockMvc.perform(put("/issues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":1,"title":"t","type":"BUG","issueStatus":"DONE",
                                 "issuePriority":"LOW","dueDate":"2026-12-01T10:00:00"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot move an issue from OPEN to DONE"));
    }

    @Test
    void getIssueById_whenNotFound_shouldReturn404() throws Exception {
        when(issueService.getIssueById(5L)).thenThrow(new ResourceNotFoundException("Issue ID not found"));

        mockMvc.perform(get("/issues/5")).andExpect(status().isNotFound());
    }

    @Test
    void getIssueById_whenAccessDenied_shouldReturn403() throws Exception {
        when(issueService.getIssueById(5L)).thenThrow(new AccessDeniedException("You are not allowed to do this"));

        mockMvc.perform(get("/issues/5")).andExpect(status().isForbidden());
    }

    @Test
    void deleteIssue_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/issues/5")).andExpect(status().isNoContent());
        verify(issueService).deleteIssue(5L);
    }
}