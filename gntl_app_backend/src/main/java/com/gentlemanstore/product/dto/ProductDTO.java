package com.gentlemanstore.product.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String categoryName;
    private List<ProductSizeDTO> sizes;
    private List<String> imageUrls;
    private List<String> tags;
}
