package com.gentlemanstore.inventory.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.inventory.dto.InventoryDTO;
import com.gentlemanstore.inventory.dto.StockAlertDTO;
import com.gentlemanstore.inventory.mapper.InventoryMapper;
import com.gentlemanstore.inventory.model.Inventory;
import com.gentlemanstore.inventory.model.StockAlert;
import com.gentlemanstore.inventory.repository.InventoryRepository;
import com.gentlemanstore.inventory.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repo;
    private final StockAlertRepository stockAlertRepository;
    private final InventoryMapper mapper;

    @Transactional(readOnly = true)
    public InventoryDTO getInventory(Long productSizeId) {
        Inventory inventory = repo.findByProductSizeIdAndDeletedFalse(productSizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
        return mapper.toDTO(inventory);
    }

    @Transactional(readOnly = true)
    public List<StockAlertDTO> getStockAlerts() {
        return stockAlertRepository.findAllByResolvedFalseAndDeletedFalse()
                .stream()
                .map(mapper::toAlertDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public void resolveAlert(Long id){
        StockAlert stockAlert = stockAlertRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock alert not found"));

        stockAlert.setResolved(true);
        stockAlertRepository.save(stockAlert);
    }

    @Transactional()
    public InventoryDTO updateQuantity(Long productSizeId, Integer newQuantity){
        Inventory inventory = repo.findByProductSizeIdAndDeletedFalse(productSizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        if (newQuantity <= inventory.getMinQuantity()) {
            StockAlert alert = StockAlert.builder()
                    .message("Low stock for product size: " + inventory.getProductSize().getSize())
                    .inventory(inventory)
                    .build();
            stockAlertRepository.save(alert);
        }

        inventory.setQuantity(newQuantity);
        repo.save(inventory);
        return mapper.toDTO(inventory);
    }

    @Transactional()
    public void deleteInventory(Long productSizeId){
        Inventory inventory = repo.findByProductSizeIdAndDeletedFalse(productSizeId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

        inventory.setDeleted(true);
        repo.save(inventory);
    }

}
