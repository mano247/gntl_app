package com.gentlemanstore.discount.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.discount.dto.CreateDiscountRequest;
import com.gentlemanstore.discount.dto.CreatePromotionRequest;
import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/discounts")
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService service;

    @GetMapping("/code/{code}")
    public ResponseEntity<ApiResponse<DiscountDTO>> getDiscount(@PathVariable String code){
        return ResponseEntity.ok(ApiResponse.success("Discount retrieved successfully", service.getDiscount(code)));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DiscountDTO>>> getAllDiscounts(){
        return ResponseEntity.ok(ApiResponse.success("Discounts retrieved successfully", service.getAllDiscounts()));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<DiscountDTO>> createDiscount(@Valid @RequestBody CreateDiscountRequest request){
        return ResponseEntity.ok(ApiResponse.success("Discount created successfully", service.createDiscount(request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDiscount(@PathVariable Long id){
        service.deleteDiscount(id);
        return ResponseEntity.ok(ApiResponse.success("Discount deleted successfully", null));
    }

    @GetMapping("/promotions/active")
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> getActivePromotions(){
        return ResponseEntity.ok(ApiResponse.success("Active promotions retrieved successfully", service.getActivePromotions()));
    }

    @PostMapping("/promotions")
    public ResponseEntity<ApiResponse<PromotionDTO>> createPromotion(@Valid @RequestBody CreatePromotionRequest request){
        return ResponseEntity.ok(ApiResponse.success("Promotion created successfully", service.createPromotion(request)));
    }

    @PostMapping("/promotions/{promotionId}/apply/{userId}")
    public ResponseEntity<ApiResponse<Void>> applyPromotion(@PathVariable Long promotionId, @PathVariable Long userId){
        service.applyPromotion(userId, promotionId);
        return ResponseEntity.ok(ApiResponse.success("Promotion applied successfully", null));
    }
}
