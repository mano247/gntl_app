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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", service.getOrder(id, currentUser)));
    }

    // Bez default sortiranja stranice su nedeterminističke (nema ORDER BY),
    // pa se ista porudžbina može pojaviti/nestati između zahteva.
    @GetMapping("/my/paged")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getMyOrdersPaged(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",
                service.getUserOrdersPaged(currentUser.getId(), status, pageable)));
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrdersPaged(
            @RequestParam(required = false) String status,
            @PageableDefault(sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully",
                service.getAllOrdersPaged(status, pageable)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'MANAGER')")
    public ResponseEntity<ApiResponse<OrderDTO>> updateStatus(
            @PathVariable Long id,
            @RequestBody String status) {
        OrderDTO dto = service.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Status updated", dto));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id, @AuthenticationPrincipal User currentUser){
        service.cancelOrder(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", null));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", service.createOrder(request, currentUser.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct (@PathVariable Long id){
        service.deleteOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order deleted successfully", null));
    }
}
