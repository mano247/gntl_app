package com.gentlemanstore.cart.controller;

import com.gentlemanstore.cart.dto.AddToCartRequest;
import com.gentlemanstore.cart.dto.CartDTO;
import com.gentlemanstore.cart.service.CartService;
import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.user.model.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService service;

    @GetMapping()
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> getCart(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully",
                service.getCart(currentUser.getId())));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> addToCart(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddToCartRequest request){
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully",
                service.addToCart(currentUser.getId(), request)));
    }

    @DeleteMapping("/items/{cartItemId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CartDTO>> removeFromCart(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long cartItemId){
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully",
                service.removeFromCart(currentUser.getId(), cartItemId)));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(
            @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok(ApiResponse.success("Checkout successful",
                service.checkout(currentUser.getId())));
    }
}
