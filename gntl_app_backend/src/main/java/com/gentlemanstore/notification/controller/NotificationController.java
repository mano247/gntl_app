package com.gentlemanstore.notification.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.notification.dto.NotificationDTO;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<NotificationDTO>>> getNotifications(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully",
                service.getNotifications(currentUser.getId(), pageable)));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved successfully",
                service.getUnreadCount(currentUser.getId())));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        service.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal User currentUser) {
        service.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}