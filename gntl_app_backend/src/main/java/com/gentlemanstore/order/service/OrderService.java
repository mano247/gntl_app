package com.gentlemanstore.order.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.order.dto.CreateOrderRequest;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.dto.OrderItemRequest;
import com.gentlemanstore.order.mapper.OrderMapper;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderItem;
import com.gentlemanstore.order.model.OrderStatus;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.product.model.Product;
import com.gentlemanstore.product.repository.ProductRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repo;
    private final ProductRepository productRepository;
    private final OrderMapper mapper;
    private final UserRepository userRepository;

    public OrderDTO getOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapper.toDTO(order);
    }

    public List<OrderDTO> getUserOrders(Long userId){
        return repo.findAllByUserIdAndDeletedFalse(userId)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public OrderDTO createOrder(CreateOrderRequest request, Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .user(user)
                .deleted(false)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findByIdAndDeletedFalse(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

            BigDecimal itemPrice = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .totalPrice(itemPrice)
                    .order(order)
                    .build();

            orderItems.add(orderItem);
            totalPrice = totalPrice.add(itemPrice);
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);

        repo.save(order);
        return mapper.toDTO(order);
    }

    public OrderDTO updateOrderStatus(Long id, String status){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.valueOf(status));

        repo.save(order);
        return mapper.toDTO(order);
    }

    public void cancelOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);

        repo.save(order);
    }

    public void deleteOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setDeleted(true);
        repo.save(order);
    }
}
