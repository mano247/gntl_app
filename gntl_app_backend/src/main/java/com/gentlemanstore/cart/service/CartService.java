package com.gentlemanstore.cart.service;

import com.gentlemanstore.cart.dto.AddToCartRequest;
import com.gentlemanstore.cart.dto.CartDTO;
import com.gentlemanstore.cart.mapper.CartMapper;
import com.gentlemanstore.cart.model.Cart;
import com.gentlemanstore.cart.model.CartItem;
import com.gentlemanstore.cart.repository.CartItemRepository;
import com.gentlemanstore.cart.repository.CartRepository;
import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.mapper.OrderMapper;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderItem;
import com.gentlemanstore.order.model.OrderStatus;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.product.model.Product;
import com.gentlemanstore.product.model.ProductSize;
import com.gentlemanstore.product.repository.ProductRepository;
import com.gentlemanstore.product.repository.ProductSizeRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductSizeRepository productSizeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    public CartDTO getCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .deleted(false)
                            .build();
                    return cartRepository.save(newCart);
                });

        CartDTO cartDTO = cartMapper.toDTO(cart);
        BigDecimal totalPrice = cart.getItems().stream()
                .filter(item -> !item.isDeleted())
                .map(item -> item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cartDTO.setTotalPrice(totalPrice);
        return cartDTO;
    }

    @Transactional()
    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .deleted(false)
                            .build();
                    return cartRepository.save(newCart);
                });

        Product product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductSize productSize = productSizeRepository.findByIdAndDeletedFalse(request.getProductSizeId())
                .orElseThrow(() -> new ResourceNotFoundException("Product size not found"));

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .productSize(productSize)
                .quantity(request.getQuantity())
                .deleted(false)
                .build();

        cartItemRepository.save(cartItem);
        return getCart(userId);
    }

    @Transactional()
    public CartDTO removeFromCart(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findByIdAndDeletedFalse(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItem.setDeleted(true);
        cartItemRepository.save(cartItem);
        return getCart(userId);
    }

    @Transactional()
    public OrderDTO checkout(Long userId) {
        Cart cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        List<CartItem> items = cartItemRepository.findAllByCartIdAndDeletedFalse(cart.getId());

        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .deleted(false)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : items) {
            BigDecimal itemPrice = cartItem.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .totalPrice(itemPrice)
                    .order(order)
                    .deleted(false)
                    .build();

            orderItems.add(orderItem);
            totalPrice = totalPrice.add(itemPrice);
        }

        order.setOrderItems(orderItems);
        order.setTotalPrice(totalPrice);
        orderRepository.save(order);

        items.forEach(item -> item.setDeleted(true));
        cartItemRepository.saveAll(items);

        return orderMapper.toDTO(order);
    }


}
