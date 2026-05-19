package com.gentlemanstore.payment.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;

    private BigDecimal amount;

    private String paymentMethod;

    private String status;

    private LocalDateTime createdAt;

    private Long orderId;
}
