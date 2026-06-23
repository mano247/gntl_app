package com.gentlemanstore.payment.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import com.gentlemanstore.payment.dto.CreatePaymentRequest;
import com.gentlemanstore.payment.dto.PaymentDTO;
import com.gentlemanstore.payment.dto.UpdatePaymentStatusRequest;
import com.gentlemanstore.payment.service.PaymentService;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", service.getPayment(id, currentUser)));
    }

    @GetMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<PaymentDTO>>> getAllPayments(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", service.getAllPayments(pageable)));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@Valid @RequestBody CreatePaymentRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Payment created successfully", service.createPayment(request, currentUser)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable Long id, @Valid @RequestBody UpdatePaymentStatusRequest request){
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", service.updatePaymentStatus(id, request.getStatus())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id){
        service.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully", null));
    }
}
