package com.gentlemanstore.inventory.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryDTO {
    private Long id;
    private Integer quantity;
    private Integer minQuantity;
    private Long productSizeId;
    private String productSizeName;
}
