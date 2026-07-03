package com.gentlemanstore.loyalty.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.loyalty.dto.AddPointsRequest;
import com.gentlemanstore.loyalty.dto.LoyaltyAccountDTO;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.service.LoyaltyService;
import com.gentlemanstore.user.model.User;
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
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService service;

    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> getAccount(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Loyalty account retrieved successfully", service.getAccount(currentUser.getId())));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<LoyaltyTransactionDTO>>> getTransactions(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Loyalty transactions retrieved successfully",
                service.getTransactions(currentUser.getId(), pageable)));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> getUserAccount(
            @PathVariable Long userId){
        return ResponseEntity.ok(ApiResponse.success("Loyalty account retrieved successfully",
                service.getAccount(userId)));
    }

    @GetMapping("/user/{userId}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<LoyaltyTransactionDTO>>> getUserTransactions(
            @PathVariable Long userId,
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Loyalty transactions retrieved successfully",
                service.getTransactions(userId, pageable)));
    }

    @PostMapping("/account")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> createAccount(@AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Loyalty account created successfully", service.createAccount(currentUser.getId())));
    }

    @PutMapping("/points")
    // MANAGER namerno uklonjen — dodela loyalty poena je dozvoljena samo ADMIN i EMPLOYEE rolama
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> addPoints(@AuthenticationPrincipal User currentUser, @Valid @RequestBody AddPointsRequest request){
        Long targetUserId = request.getUserId() != null ? request.getUserId() : currentUser.getId();
        return ResponseEntity.ok(ApiResponse.success("Loyalty points added successfully",
                service.addPoints(targetUserId, request.getPoints(), request.getDescription())));
    }

}
