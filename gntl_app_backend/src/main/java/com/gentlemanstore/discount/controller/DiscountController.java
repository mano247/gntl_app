package com.gentlemanstore.discount.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.discount.dto.CreateDiscountRequest;
import com.gentlemanstore.discount.dto.CreatePromotionRequest;
import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.service.DiscountService;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService service;

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<DiscountDTO>> getDiscount(@PathVariable String code){
        return ResponseEntity.ok(ApiResponse.success("Discount retrieved successfully", service.getDiscount(code)));
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<DiscountDTO>>> getAllDiscounts(){
        return ResponseEntity.ok(ApiResponse.success("Discounts retrieved successfully", service.getAllDiscounts()));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<DiscountDTO>> createDiscount(@Valid @RequestBody CreateDiscountRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Discount created successfully", service.createDiscount(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable Long id){
        service.deleteDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Discount deleted successfully", null));
    }

    @GetMapping("/promotions/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> getActivePromotions(){
        return ResponseEntity.ok(ApiResponse.success("Active promotions retrieved successfully", service.getActivePromotions()));
    }

    @PostMapping("/promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PromotionDTO>> createPromotion(@Valid @RequestBody CreatePromotionRequest request){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created successfully", service.createPromotion(request)));
    }

    @PostMapping("/promotions/{promotionId}/apply/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> applyPromotion(@PathVariable Long promotionId, @PathVariable Long userId, @AuthenticationPrincipal User currentUser){
        service.applyPromotion(userId, promotionId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Promotion applied successfully", null));
    }

    @GetMapping("/validate/{code}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<DiscountDTO>> validatePromoCode(
            @PathVariable String code,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ApiResponse.success("Promo code valid",
                service.validateAndUsePromoCode(code, currentUser.getId())));
    }
}
