package com.gentlemanstore.inventory.mapper;

import com.gentlemanstore.inventory.dto.InventoryDTO;
import com.gentlemanstore.inventory.dto.StockAlertDTO;
import com.gentlemanstore.inventory.model.Inventory;
import com.gentlemanstore.inventory.model.StockAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    @Mapping(source = "productSize.id", target = "productSizeId")
    @Mapping(source = "productSize.size", target = "productSizeName")
    InventoryDTO toDTO(Inventory inventory);

    @Mapping(source = "inventory.id", target = "inventoryId")
    StockAlertDTO toAlertDTO(StockAlert stockAlert);
}
