package com.gentlemanstore.cart.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDTO {
    private Long id;
    private String productName;
    private Long productId;
    private String imageUrl;
    private String size;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal originalPrice;
}
