package com.gentlemanstore.notification.repository;

import com.gentlemanstore.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    long countByUserIdAndIsReadFalseAndDeletedFalse(Long userId);
    Optional<Notification> findByIdAndDeletedFalse(Long id);
}