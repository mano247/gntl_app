package com.gentlemanstore.notification.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.notification.dto.CreateNotificationRequest;
import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyNotifications(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully",
                service.getUserNotifications(currentUser.getId())));
    }

    @GetMapping("/my/unread")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getMyUnreadNotifications(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully",
                service.getUnreadNotifications(currentUser.getId())));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUserNotifications(
            @PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully",
                service.getUserNotifications(userId)));
    }

    @GetMapping("/{userId}/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getUnreadNotifications(@PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("Unreaded users notifications retrieved successfully", service.getUnreadNotifications(userId)));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<NotificationDTO>> createNotification(@Valid @RequestBody CreateNotificationRequest request){
        return ResponseEntity.ok(ApiResponse.success("Notification created successfully", service.createNotification(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read successfully", service.markAsRead(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long id){
        service.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}
