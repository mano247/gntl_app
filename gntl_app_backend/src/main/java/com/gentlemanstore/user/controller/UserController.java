package com.gentlemanstore.user.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.user.dto.UpdateUserRequest;
import com.gentlemanstore.user.dto.UserDTO;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
            @RequestParam(required = false) Boolean deleted,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully",
                service.getAllUsers(pageable, deleted)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<UserDTO>> getById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", service.getProfile(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile (@PathVariable Long id,@Valid @RequestBody UpdateUserRequest request){
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", service.updateProfile(id, request)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> getMyProfile(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully",
                service.getProfile(currentUser.getId())));
    }

    @PutMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserRequest request){
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully",
                service.updateProfile(currentUser.getId(), request)));
    }

    @DeleteMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal User currentUser){
        service.deleteAccount(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Account deleted successfully", null));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProfile (@PathVariable Long id){
        service.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> changeRole(@PathVariable Long id, @RequestBody String roleName) {
        return ResponseEntity.ok(ApiResponse.success("Role changed successfully", service.changeRole(id, roleName)));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> reactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User reactivated successfully", service.reactivateUser(id)));
    }

}
