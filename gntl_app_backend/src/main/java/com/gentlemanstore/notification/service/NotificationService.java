package com.gentlemanstore.notification.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.model.Notification;
import com.gentlemanstore.notification.model.NotificationType;
import com.gentlemanstore.notification.repository.NotificationRepository;
import com.gentlemanstore.user.model.RoleName;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(Long userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .deleted(false)
                .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public void createNotificationForAllCustomers(String title, String message, NotificationType type) {
        List<User> customers = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted() && u.getRoles().stream()
                        .anyMatch(r -> r.getName() == RoleName.ROLE_CUSTOMER))
                .toList();

        List<Notification> notifications = customers.stream()
                .map(user -> Notification.builder()
                        .user(user)
                        .title(title)
                        .message(message)
                        .type(type)
                        .isRead(false)
                        .deleted(false)
                        .build())
                .toList();

        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndDeletedFalse(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        Page<Notification> notifications = notificationRepository
                .findAllByUserIdAndDeletedFalse(userId, Pageable.unpaged());
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications.getContent());
    }

    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findAllByUserIdAndDeletedFalse(userId, pageable)
                .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(userId);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}