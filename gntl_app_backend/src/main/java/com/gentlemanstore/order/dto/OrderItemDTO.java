package com.gentlemanstore.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDTO {
    private Long id;
    private String productName;
    private String imageUrl;
    private String size;
    private Integer quantity;
    private BigDecimal price;
}
