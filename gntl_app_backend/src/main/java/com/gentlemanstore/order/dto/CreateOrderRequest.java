package com.gentlemanstore.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @NotNull(message = "Address is required")
    private Long adressId;

    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItemRequest> items;

}
