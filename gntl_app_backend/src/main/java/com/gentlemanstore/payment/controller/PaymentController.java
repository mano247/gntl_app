package com.gentlemanstore.payment.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.payment.dto.CreatePaymentRequest;
import com.gentlemanstore.payment.dto.PaymentDTO;
import com.gentlemanstore.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentDTO>> getPayment(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", service.getPayment(id)));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<PaymentDTO>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", service.getAllPayments()));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@Valid @RequestBody CreatePaymentRequest request){
        return ResponseEntity.ok(ApiResponse.success("Payment created successfully", service.createPayment(request)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PaymentDTO>> updatePayment(@PathVariable Long id, @RequestBody String status){
        return ResponseEntity.ok(ApiResponse.success("Payment status updated successfully", service.updatePaymentStatus(id, status)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable Long id){
        service.deletePayment(id);
        return ResponseEntity.ok(ApiResponse.success("Payment deleted successfully", null));
    }
}
