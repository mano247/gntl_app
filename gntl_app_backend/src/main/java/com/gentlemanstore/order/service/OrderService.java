package com.gentlemanstore.order.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.common.util.EmailService;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.service.LoyaltyService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repo;
    private final ProductRepository productRepository;
    private final OrderMapper mapper;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final LoyaltyService loyaltyService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long id) {
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

        try {
            int points = order.getTotalPrice().divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN).intValue();
            log.info("Adding {} loyalty points for user {}", points, userId);
            if (points > 0) {
                loyaltyService.addPoints(userId, points, "Purchase #" + order.getId());
            }
        } catch (Exception e) {
            log.warn("Loyalty points adding failed: {}", e.getMessage());
        }

        try {
            emailService.sendOrderConfirmationEmail(
                    order.getUser().getEmail(),
                    order.getUser().getFirstName(),
                    order.getId(),
                    order.getTotalPrice()
            );
        } catch (Exception e) {
            log.warn("Order confirmation email sending failed: {}", e.getMessage());
        }

        return mapper.toDTO(order);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status){
        Order order = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.trim().replace("\"", "").toUpperCase());
            order.setStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order status: " + status);
        }

        Order savedOrder = repo.save(order);
        return mapper.toDTO(savedOrder);
    }

//    public void sendStatusEmailAsync(Order order, String status) {
//        try {
//            emailService.sendOrderStatusEmail(
//                    order.getUser().getEmail(),
//                    order.getUser().getFirstName(),
//                    order.getId(),
//                    status
//            );
//        } catch (Exception e) {
//            log.warn("Order status email sending failed: {}", e.getMessage());
//        }
//    }

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

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrdersPaged(Pageable pageable) {
        return repo.findAllByDeletedFalse(pageable)
                .map(mapper::toDTO);
    }

    private void applyLoyaltyDiscount(OrderDTO orderDTO, Long userId) {
        try {
            loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId).ifPresent(account -> {
                BigDecimal discountPct = account.getLoyaltyTier().getDiscountPercentage();
                BigDecimal totalPrice = orderDTO.getTotalPrice();
                if (discountPct != null && discountPct.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = totalPrice.multiply(discountPct).divide(BigDecimal.valueOf(100));
                    orderDTO.setLoyaltyDiscount(discount);
                    orderDTO.setFinalPrice(totalPrice.subtract(discount));
                } else {
                    orderDTO.setLoyaltyDiscount(BigDecimal.ZERO);
                    orderDTO.setFinalPrice(totalPrice);
                }
            });
        } catch (Exception e) {
            log.warn("Loyalty discount calculation failed: {}", e.getMessage());
        }
    }
}
