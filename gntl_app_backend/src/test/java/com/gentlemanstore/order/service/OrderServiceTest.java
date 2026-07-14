package com.gentlemanstore.order.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.common.util.EmailService;
import com.gentlemanstore.order.dto.CreateOrderRequest;
import com.gentlemanstore.order.dto.OrderDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        com.gentlemanstore.user.model.User owner = new com.gentlemanstore.user.model.User();
        owner.setId(1L);

        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .user(owner)
                .deleted(false)
                .build();

        when(repo.findById(1L)).thenReturn(Optional.of(order));

        // when — vlasnik otkazuje sopstvenu porudzbinu
        orderService.cancelOrder(1L, owner);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldFilterAllOrdersByStatusWhenStatusProvided() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).deleted(false).build();
        when(repo.findAllByStatusAndDeletedFalse(OrderStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));
        when(mapper.toDTO(order)).thenReturn(new OrderDTO());

        // when
        Page<OrderDTO> result = orderService.getAllOrdersPaged("PENDING", pageable);

        // then — koristi se filtrirani upit, ne nefiltrirani
        assertThat(result.getTotalElements()).isEqualTo(1L);
        verify(repo, never()).findAllByDeletedFalse(any(Pageable.class));
    }

    @Test
    void shouldNormalizeStatusFilterCaseAndWhitespace() {
        // given — frontend/klijent može poslati status u malim slovima sa razmacima
        Pageable pageable = PageRequest.of(0, 20);
        when(repo.findAllByStatusAndDeletedFalse(OrderStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        Page<OrderDTO> result = orderService.getAllOrdersPaged(" pending ", pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(0L);
        verify(repo).findAllByStatusAndDeletedFalse(OrderStatus.PENDING, pageable);
    }

    @Test
    void shouldReturnAllOrdersWhenStatusNotProvided() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(repo.findAllByDeletedFalse(pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        orderService.getAllOrdersPaged(null, pageable);

        // then
        verify(repo).findAllByDeletedFalse(pageable);
        verify(repo, never()).findAllByStatusAndDeletedFalse(any(OrderStatus.class), any(Pageable.class));
    }

    @Test
    void shouldThrowBadRequestForInvalidStatusFilter() {
        Pageable pageable = PageRequest.of(0, 20);

        assertThrows(BadRequestException.class, () ->
                orderService.getAllOrdersPaged("NOT_A_STATUS", pageable));
    }

    @Test
    void shouldFilterUserOrdersByStatusWhenStatusProvided() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        when(repo.findAllByUserIdAndStatusAndDeletedFalse(5L, OrderStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        orderService.getUserOrdersPaged(5L, "PENDING", pageable);

        // then
        verify(repo).findAllByUserIdAndStatusAndDeletedFalse(5L, OrderStatus.PENDING, pageable);
        verify(repo, never()).findAllByUserIdAndDeletedFalse(any(Long.class), any(Pageable.class));
    }
}
