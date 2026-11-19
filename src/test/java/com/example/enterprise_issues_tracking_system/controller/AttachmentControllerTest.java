package com.example.enterprise_issues_tracking_system.controller;

import com.example.enterprise_issues_tracking_system.security.JWTFilter;
import com.example.enterprise_issues_tracking_system.service.AttachmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AttachmentController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JWTFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AttachmentControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    AttachmentService attachmentService;

    @Test
    void createAttachment_whenFilePartMissing_shouldReturn400() throws Exception {
        mockMvc.perform(multipart("/issues/1/attachments"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(attachmentService);
    }
}