package com.gentlemanstore.order.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.common.util.EmailService;
import com.gentlemanstore.order.dto.CreateOrderRequest;
import com.gentlemanstore.order.mapper.OrderMapper;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderStatus;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {
    @Mock
    private OrderRepository repo;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderMapper mapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of());

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when + then
        assertThrows(ResourceNotFoundException.class, () -> {
            orderService.createOrder(request, 1L);
        });
    }

    @Test
    void shouldCancelOrderSuccessfully() {
        // given
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .deleted(false)
                .build();

        when(repo.findById(1L)).thenReturn(Optional.of(order));

        // when
        orderService.cancelOrder(1L);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
