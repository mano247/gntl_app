package com.gentlemanstore.inventory.controller;

import com.gentlemanstore.common.response.ApiResponse;
import com.gentlemanstore.inventory.dto.InventoryDTO;
import com.gentlemanstore.inventory.dto.StockAlertDTO;
import com.gentlemanstore.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<InventoryDTO>> getById(@PathVariable Long id){
        return ResponseEntity.ok(ApiResponse.success("Inventory retrieved successfully", service.getInventory(id)));
    }

    @GetMapping("/stock_alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<StockAlertDTO>>> getStockAlerts(){
        return ResponseEntity.ok(ApiResponse.success("Stock alerts retrieved successfully", service.getStockAlerts()));
    }

    @PutMapping("/{id}/resolve_alert")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<Void>> resolveAlert(@PathVariable Long id){
        service.resolveAlert(id);
        return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully", null));
    }

    @PutMapping("/{id}/update_quantity")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateQuantity(@PathVariable Long id, @RequestBody Integer newQuantity){
        return ResponseEntity.ok(ApiResponse.success("Quantity updated successfully", service.updateQuantity(id, newQuantity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteInventory (@PathVariable Long id){
        service.deleteInventory(id);
        return ResponseEntity.ok(ApiResponse.success("Inventory deleted successfully", null));
    }
}
