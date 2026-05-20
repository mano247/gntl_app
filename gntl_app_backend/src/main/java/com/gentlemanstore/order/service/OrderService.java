package com.gentlemanstore.order.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.common.util.EmailService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapper.toDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getUserOrdersPaged(Long userId, Pageable pageable) {
        return repo.findAllByUserIdAndDeletedFalse(userId, pageable)
                .map(mapper::toDTO);
    }

    @Transactional()
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

        emailService.sendOrderConfirmationEmail(
                order.getUser().getEmail(),
                order.getUser().getFirstName(),
                order.getId(),
                order.getTotalPrice()
        );

        return mapper.toDTO(order);
    }

    @Transactional()
    public OrderDTO updateOrderStatus(Long id, String status){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.valueOf(status));

        repo.save(order);

        emailService.sendOrderStatusEmail(
                order.getUser().getEmail(),
                order.getUser().getFirstName(),
                order.getId(),
                status
        );

        return mapper.toDTO(order);
    }

    @Transactional()
    public void cancelOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);

        repo.save(order);
    }

    @Transactional()
    public void deleteOrder(Long id){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setDeleted(true);
        repo.save(order);
    }
}
