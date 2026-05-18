package com.gentlemanstore.loyalty.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.loyalty.dto.AddPointsRequest;
import com.gentlemanstore.loyalty.dto.LoyaltyAccountDTO;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.service.LoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> getAccount(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Loyalty account retrieved successfully", service.getAccount(id)));
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<ApiResponse<List<LoyaltyTransactionDTO>>> getTransactions(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Loyalty transactions retrieved successfully", service.getTransactions(id)));
    }

    @PostMapping("/{id}/account")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> createAccount(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Loyalty account created successfully", service.createAccount(id)));
    }

    @PutMapping("/{id}/points")
    public ResponseEntity<ApiResponse<LoyaltyAccountDTO>> addPoints(@PathVariable Long id, @RequestBody AddPointsRequest request){
        return ResponseEntity.ok(ApiResponse.success("Loyalty points added successfully",
                service.addPoints(id, request.getPoints(), request.getDescription())));
    }

}
