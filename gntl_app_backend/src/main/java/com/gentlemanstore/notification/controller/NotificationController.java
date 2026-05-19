package com.gentlemanstore.notification.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.notification.dto.CreateNotificationRequest;
import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(@PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("User notifications retrieved successfully", service.getUserNotifications(userId)));
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(@PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("Unreaded users notifications retrieved successfully", service.getUnreadNotifications(userId)));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<NotificationDTO>> createNotification(@Valid @RequestBody CreateNotificationRequest request){
        return ResponseEntity.ok(ApiResponse.success("Notification created successfully", service.createNotification(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read successfully", service.markAsRead(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id){
        service.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}
