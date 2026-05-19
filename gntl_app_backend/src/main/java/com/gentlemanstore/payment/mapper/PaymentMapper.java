package com.gentlemanstore.payment.mapper;

import com.gentlemanstore.payment.dto.PaymentDTO;
import com.gentlemanstore.payment.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "order.id", target = "orderId")
    PaymentDTO toDTO(Payment payment);
}
