package com.gentlemanstore.product.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSizeDTO {
    private Long id;
    private String size;
    private Integer quantity;
}