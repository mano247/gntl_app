package com.gentlemanstore.cart.service;

import com.gentlemanstore.cart.dto.AddToCartRequest;
import com.gentlemanstore.cart.dto.CartDTO;
import com.gentlemanstore.cart.dto.CheckoutRequest;
import com.gentlemanstore.cart.mapper.CartMapper;
import com.gentlemanstore.cart.model.Cart;
import com.gentlemanstore.cart.model.CartItem;
import com.gentlemanstore.cart.repository.CartItemRepository;
import com.gentlemanstore.cart.repository.CartRepository;
import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.discount.service.DiscountService;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.service.LoyaltyService;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.mapper.OrderMapper;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderItem;
import com.gentlemanstore.order.model.OrderStatus;
import com.gentlemanstore.order.model.Shipment;
import com.gentlemanstore.order.repository.OrderRepository;
import com.gentlemanstore.order.repository.ShipmentRepository;
import com.gentlemanstore.product.model.Product;
import com.gentlemanstore.product.model.ProductSize;
import com.gentlemanstore.product.repository.ProductRepository;
import com.gentlemanstore.product.repository.ProductSizeRepository;
import com.gentlemanstore.user.model.Address;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.AddressRepository;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
    private final AddressRepository addressRepository;
    private final ShipmentRepository shipmentRepository;
    private final LoyaltyService loyaltyService;
    private final DiscountRepository discountRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final DiscountService discountService;

    @Transactional()
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

        List<CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isDeleted())
                .collect(Collectors.toList());

        CartDTO cartDTO = cartMapper.toDTO(cart);
        cartDTO.setItems(activeItems.stream()
                .map(cartMapper::toItemDTO)
                .collect(Collectors.toList()));

        BigDecimal totalPrice = activeItems.stream()
                .map(item -> (item.getUnitPrice() != null ? item.getUnitPrice() : item.getProduct().getPrice())
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cartDTO.setTotalPrice(totalPrice);
        // Loyalty popust
        try {
            loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId).ifPresent(account -> {
                BigDecimal discountPct = account.getLoyaltyTier().getDiscountPercentage();
                if (discountPct != null && discountPct.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal discount = totalPrice.multiply(discountPct).divide(BigDecimal.valueOf(100));
                    cartDTO.setLoyaltyDiscount(discount);
                    cartDTO.setFinalPrice(totalPrice.subtract(discount));
                } else {
                    cartDTO.setLoyaltyDiscount(BigDecimal.ZERO);
                    cartDTO.setFinalPrice(totalPrice);
                }
            });
        } catch (Exception e) {
            cartDTO.setLoyaltyDiscount(BigDecimal.ZERO);
            cartDTO.setFinalPrice(totalPrice);
        }

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

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        BigDecimal unitPrice = product.getPrice();
        java.util.Optional<com.gentlemanstore.discount.model.Discount> activeDiscount =
                discountRepository.findActiveDiscountForProduct(
                        product.getId(), product.getCategory().getId(), now
                );
        if (activeDiscount.isPresent()) {
            com.gentlemanstore.discount.model.Discount discount = activeDiscount.get();
            if (discount.getDiscountType() == com.gentlemanstore.discount.model.DiscountType.PERCENTAGE) {
                BigDecimal multiplier = BigDecimal.ONE.subtract(
                        discount.getValue().divide(BigDecimal.valueOf(100))
                );
                unitPrice = product.getPrice().multiply(multiplier);
            } else {
                unitPrice = product.getPrice().subtract(discount.getValue());
            }
        }

        CartItem cartItem = CartItem.builder()
                .cart(cart)
                .product(product)
                .productSize(productSize)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
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
    public OrderDTO checkout(Long userId, CheckoutRequest request) {
        Cart cart = cartRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        List<CartItem> items = cartItemRepository.findAllByCartIdAndDeletedFalse(cart.getId());

        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Address address = addressRepository.findByIdAndDeletedFalse(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found");
        }

        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.PENDING)
                .deleted(false)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : items) {
            BigDecimal itemPrice = (cartItem.getUnitPrice() != null ? cartItem.getUnitPrice() : cartItem.getProduct().getPrice())
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .productSize(cartItem.getProductSize())
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

        // Loyalty popust
        final BigDecimal finalTotalPrice = totalPrice;
        loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId).ifPresent(account -> {
            BigDecimal discountPct = account.getLoyaltyTier().getDiscountPercentage();
            if (discountPct != null && discountPct.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal discount = finalTotalPrice.multiply(discountPct).divide(BigDecimal.valueOf(100));
                order.setLoyaltyDiscount(discount);
                order.setFinalPrice(finalTotalPrice.subtract(discount));
            } else {
                order.setLoyaltyDiscount(BigDecimal.ZERO);
                order.setFinalPrice(finalTotalPrice);
            }
        });

        // Promo kod popust
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            try {
                DiscountDTO promoDiscount = discountService.validateAndUsePromoCode(
                        request.getPromoCode(), userId
                );

                BigDecimal basePrice = order.getFinalPrice() != null ?
                        order.getFinalPrice() : order.getTotalPrice();

                BigDecimal promoDiscountAmount;
                if (promoDiscount.getDiscountType().equals("PERCENTAGE")) {
                    promoDiscountAmount = basePrice.multiply(promoDiscount.getValue())
                            .divide(BigDecimal.valueOf(100));
                } else {
                    promoDiscountAmount = promoDiscount.getValue();
                }

                order.setPromoDiscount(promoDiscountAmount);
                order.setFinalPrice(basePrice.subtract(promoDiscountAmount));

                discountService.markPromoCodeAsUsed(request.getPromoCode(), userId);
            } catch (Exception e) {
                throw new BadRequestException("Invalid promo code: " + e.getMessage());
            }
        }

        orderRepository.save(order);

        try {
            int points = order.getTotalPrice().divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.DOWN).intValue();
            log.info("Adding {} loyalty points for user {}", points, userId);
            if (points > 0) {
                loyaltyService.addPoints(userId, points, "Purchase #" + order.getId());
            }
        } catch (Exception e) {
            log.warn("Loyalty points adding failed: {}", e.getMessage());
        }

        String formattedAddress = formatAddress(address);
        Shipment shipment = Shipment.builder()
                .trackingNumber(generateTrackingNumber())
                .shippingAddress(formattedAddress)
                .order(order)
                .deleted(false)
                .build();
        shipmentRepository.save(shipment);

        items.forEach(item -> item.setDeleted(true));
        cartItemRepository.saveAll(items);

        return orderMapper.toDTO(order);
    }

    private String formatAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getStreet());
        if (address.getApartment() != null && !address.getApartment().isBlank()) {
            sb.append(", ").append(address.getApartment());
        }
        sb.append(", ").append(address.getCity());
        sb.append(", ").append(address.getPostalCode());
        sb.append(", ").append(address.getCountry());
        return sb.toString();
    }

    private String generateTrackingNumber() {
        return "GS-" + System.currentTimeMillis();
    }


}
