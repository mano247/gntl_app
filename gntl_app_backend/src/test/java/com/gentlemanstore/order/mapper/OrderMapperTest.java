package com.gentlemanstore.order.mapper;

import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderStatus;
import com.gentlemanstore.user.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderMapperTest {

    private final OrderMapper mapper = Mappers.getMapper(OrderMapper.class);

    @Test
    void dtoCarriesCustomerNameAndEmailForStaffView() {
        User customer = new User();
        customer.setId(10L);
        customer.setFirstName("Petar");
        customer.setLastName("Petrović");
        customer.setEmail("petar@example.com");

        Order order = Order.builder()
                .id(38L)
                .totalPrice(new BigDecimal("150.00"))
                .status(OrderStatus.DELIVERED)
                .user(customer)
                .build();

        OrderDTO dto = mapper.toDTO(order);

        assertEquals("Petar Petrović", dto.getCustomerName());
        assertEquals("petar@example.com", dto.getCustomerEmail());
        assertEquals("DELIVERED", dto.getStatus());
    }
}
