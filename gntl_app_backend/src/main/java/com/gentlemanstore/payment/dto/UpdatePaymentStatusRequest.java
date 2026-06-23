package com.gentlemanstore.payment.dto;

import com.gentlemanstore.payment.model.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentStatusRequest {
    @NotNull(message = "Status is required")
    private PaymentStatus status;
}
