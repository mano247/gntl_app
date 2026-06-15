package com.gentlemanstore.product.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.product.dto.CategoryDTO;
import com.gentlemanstore.product.dto.CreateProductRequest;
import com.gentlemanstore.product.dto.ProductDTO;
import com.gentlemanstore.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService service;

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAll(Pageable pageable){
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", service.getAllProducts(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDTO>> getById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", service.getProduct(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(@PathVariable Long id,@Valid @RequestBody CreateProductRequest request){
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", service.updateProduct(id, request)));
    }

    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody CreateProductRequest request){
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", service.createProduct(request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct (@PathVariable Long id){
        service.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CUSTOMER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories(){
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved successfully", service.getAllCategories()));
    }
}
