package com.gentlemanstore.order.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.order.dto.CreateOrderRequest;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.service.OrderService;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", service.getOrder(id)));
    }

    @GetMapping("/my/paged")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyOrdersPaged(
            @AuthenticationPrincipal User currentUser,
            Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",
                service.getUserOrdersPaged(currentUser.getId(), pageable)));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrdersPaged(Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",
                service.getAllOrdersPaged(pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(@PathVariable Long id,@RequestBody  String status){
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully", service.updateOrderStatus(id, status)));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id){
        service.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", null));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", service.createOrder(request, currentUser.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct (@PathVariable Long id){
        service.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
    }
}
