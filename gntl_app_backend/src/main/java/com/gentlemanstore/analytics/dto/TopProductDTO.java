package com.gentlemanstore.analytics.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopProductDTO {
    private String productName;
    private Integer totalSold;
}
