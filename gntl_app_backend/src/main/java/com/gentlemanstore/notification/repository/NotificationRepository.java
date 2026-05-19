package com.gentlemanstore.notification.repository;

import com.gentlemanstore.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByUserIdAndDeletedFalse(Long userId);
    List<Notification> findAllByUserIdAndReadFalseAndDeletedFalse(Long userId);
}
