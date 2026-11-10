package com.example.enterprise_issues_tracking_system.service;

import com.example.enterprise_issues_tracking_system.dto.NotificationResponse;
import com.example.enterprise_issues_tracking_system.entity.Notification;
import com.example.enterprise_issues_tracking_system.exception.ResourceNotFoundException;
import com.example.enterprise_issues_tracking_system.mapper.NotificationMapper;
import com.example.enterprise_issues_tracking_system.repository.NotificationRepository;
import com.example.enterprise_issues_tracking_system.security.CurrentUserProvider;
import com.example.enterprise_issues_tracking_system.util.PageableValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Set<String> SORTED_FIELDS = Set.of("id", "createdAt", "type");
    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final CurrentUserProvider currentUserProvider;

    public Page<NotificationResponse> getMyNotifications(boolean unreadOnly, Pageable pageable) {
        PageableValidator.requireSortableBy(pageable, SORTED_FIELDS);
        Page<Notification> page = unreadOnly ? notificationRepository.findAllByUserIdAndReadFalse(userId(), pageable) : notificationRepository.findAllByUserId(userId(), pageable);
        return page.map(notificationMapper::toResponse);
    }

    public long countUnread() {
        return notificationRepository.countByUserIdAndReadFalse(userId());
    }
    @Transactional
    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findByIdAndUserId(id, userId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification ID not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.markAllAsRead(userId());
    }

    private Long userId() {
        return currentUserProvider.getCurrentUser().getId();
    }
}
