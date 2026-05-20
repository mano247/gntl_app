package com.gentlemanstore.notification.repository;

import com.gentlemanstore.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findAllByUserIdAndDeletedFalse(Long userId, Pageable pageable);
    Page<Notification> findAllByUserIdAndReadFalseAndDeletedFalse(Long userId, Pageable pageable);
}
