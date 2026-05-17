package com.gentlemanstore.user.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.user.dto.UpdateUserRequest;
import com.gentlemanstore.user.dto.UserDTO;
import com.gentlemanstore.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAll(){
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", service.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", service.getProfile(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile (@PathVariable Long id,@Valid @RequestBody UpdateUserRequest request){
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", service.updateProfile(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProfile (@PathVariable Long id){
        service.deleteAccount(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDTO>> changeRole(@PathVariable Long id, @RequestBody String roleName) {
        return ResponseEntity.ok(ApiResponse.success("Role changed successfully", service.changeRole(id, roleName)));
    }
}
