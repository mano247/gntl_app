package com.gentlemanstore.payment.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import com.gentlemanstore.payment.dto.CreatePaymentRequest;
import com.gentlemanstore.payment.dto.PaymentDTO;
import com.gentlemanstore.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", service.getPayment(id)));
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<PaymentDTO>>> getAllPayments(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", service.getAllPayments(pageable)));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@Valid @RequestBody CreatePaymentRequest request){
        return ResponseEntity.ok(ApiResponse.success("Payment created successfully", service.createPayment(request)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable Long id, @RequestBody String status){
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", service.updatePaymentStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id){
        service.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully", null));
    }
}
