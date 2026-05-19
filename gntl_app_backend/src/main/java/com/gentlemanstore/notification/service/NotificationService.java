package com.gentlemanstore.notification.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.notification.dto.CreateNotificationRequest;
import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.mapper.NotificationMapper;
import com.gentlemanstore.notification.model.Notification;
import com.gentlemanstore.notification.model.NotificationType;
import com.gentlemanstore.notification.repository.NotificationRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;

    public List<NotificationDTO> getUserNotifications(Long userId){
        return repo.findAllByUserIdAndDeletedFalse(userId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getUnreadNotifications(Long userId){
        return repo.findAllByUserIdAndReadFalseAndDeletedFalse(userId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public NotificationDTO createNotification(CreateNotificationRequest request){
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .notificationType(NotificationType.valueOf(request.getNotificationType()))
                .user(user)
                .deleted(false)
                .build();

        repo.save(notification);
        return mapper.toDTO(notification);
    }

    public NotificationDTO markAsRead(Long id){
        Notification notification = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setRead(true);
        repo.save(notification);
        return mapper.toDTO(notification);
    }

    public void deleteNotification(Long id){
        Notification notification = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        notification.setDeleted(true);
        repo.save(notification);
    }
}
