package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.NotificationResponse;
import com.example.enterprise_issues_tracking_system.entity.Notification;
import com.example.enterprise_issues_tracking_system.entity.User;
import com.example.enterprise_issues_tracking_system.exception.InvalidRequestException;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.NotificationMapper;
import com.example.enterprise_issues_tracking_system.repository.NotificationRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long CALLER_ID = 2L;
    private static final Long NOTIFICATION_ID = 5L;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User caller;

    @BeforeEach
    void setUp() {
        caller = new User();
        caller.setId(CALLER_ID);
    }

    private void givenCaller() {
        when(currentUserProvider.getCurrentUser()).thenReturn(caller);
    }

    @Test
    public void getMyNotifications_whenAll_shouldReturnTheCallersPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = new Notification();
        NotificationResponse response = new NotificationResponse();
        givenCaller();
        when(notificationRepository.findAllByUserId(CALLER_ID, pageable)).thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        Page<NotificationResponse> result = notificationService.getMyNotifications(false, pageable);

        assertEquals(List.of(response), result.getContent());
        verify(notificationRepository, never()).findAllByUserIdAndReadFalse(any(), any());
    }

    @Test
    public void getMyNotifications_whenUnreadOnly_shouldUseTheUnreadQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification notification = new Notification();
        NotificationResponse response = new NotificationResponse();
        givenCaller();
        when(notificationRepository.findAllByUserIdAndReadFalse(CALLER_ID, pageable)).thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        Page<NotificationResponse> result = notificationService.getMyNotifications(true, pageable);

        assertEquals(List.of(response), result.getContent());
        verify(notificationRepository, never()).findAllByUserId(any(), any());
    }

    @Test
    public void getMyNotifications_whenSortFieldNotAllowed_shouldThrowException() {
        givenCaller();

        assertThrows(InvalidRequestException.class,
                () -> notificationService.getMyNotifications(false, PageRequest.of(0, 10, Sort.by("message"))));
        verify(notificationRepository, never()).findAllByUserId(any(), any());
    }

    @Test
    public void countUnread_shouldCountOnlyTheCallersUnreadNotifications() {
        givenCaller();
        when(notificationRepository.countByUserIdAndReadFalse(CALLER_ID)).thenReturn(3L);

        assertEquals(3L, notificationService.countUnread());
    }

    @Test
    public void markAsRead_whenOwnUnreadNotification_shouldMarkAndSave() {
        Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setRead(false);
        givenCaller();
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, CALLER_ID)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(NOTIFICATION_ID);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    public void markAsRead_whenAlreadyRead_shouldNotSaveAgain() {
        Notification notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setRead(true);
        givenCaller();
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, CALLER_ID)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(NOTIFICATION_ID);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    public void markAsRead_whenMissingOrOwnedBySomeoneElse_shouldThrowNotFound() {
        givenCaller();
        when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, CALLER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(NOTIFICATION_ID));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    public void markAllAsRead_shouldUpdateOnlyTheCallersNotifications() {
        givenCaller();

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsRead(CALLER_ID);
    }
}