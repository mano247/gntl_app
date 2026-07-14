package com.gentlemanstore.cart.service;

import com.gentlemanstore.cart.dto.CheckoutRequest;
import com.gentlemanstore.cart.mapper.CartMapper;
import com.gentlemanstore.cart.model.Cart;
import com.gentlemanstore.cart.model.CartItem;
import com.gentlemanstore.cart.repository.CartItemRepository;
import com.gentlemanstore.cart.repository.CartRepository;
import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.discount.service.DiscountService;
import com.gentlemanstore.loyalty.reporitory.LoyaltyAccountRepository;
import com.gentlemanstore.loyalty.service.LoyaltyService;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.mapper.OrderMapper;
import com.gentlemanstore.order.model.Order;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CartServiceCheckoutTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductSizeRepository productSizeRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartMapper cartMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private AddressRepository addressRepository;
    @Mock private ShipmentRepository shipmentRepository;
    @Mock private LoyaltyService loyaltyService;
    @Mock private DiscountRepository discountRepository;
    @Mock private LoyaltyAccountRepository loyaltyAccountRepository;
    @Mock private DiscountService discountService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CartService cartService;

    private final Long userId = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);

        Cart cart = Cart.builder().id(10L).user(user).deleted(false).build();

        Product product = Product.builder().id(5L).name("Suit").price(BigDecimal.valueOf(1000)).build();
        ProductSize size = ProductSize.builder().id(7L).size("L").quantity(5).product(product).build();
        CartItem item = CartItem.builder()
                .id(20L)
                .cart(cart)
                .product(product)
                .productSize(size)
                .quantity(2)
                .unitPrice(BigDecimal.valueOf(1000))
                .deleted(false)
                .build();

        Address address = Address.builder().id(3L).user(user).street("Main 1").city("Belgrade")
                .postalCode("11000").country("Serbia").build();

        // lenient — pojedini testovi (odbijen promo kod, addToCart) ne prolaze kroz sve stubove
        lenient().when(cartRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.of(cart));
        lenient().when(cartItemRepository.findAllByCartIdAndDeletedFalse(10L)).thenReturn(List.of(item));
        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        lenient().when(addressRepository.findByIdAndDeletedFalse(3L)).thenReturn(Optional.of(address));
        lenient().when(loyaltyAccountRepository.findByUserIdAndDeletedFalse(userId)).thenReturn(Optional.empty());
        lenient().when(orderMapper.toDTO(any(Order.class))).thenReturn(new OrderDTO());
    }

    @Test
    void checkoutComputesFinalPriceOnBackendWithoutPromo() {
        CheckoutRequest request = CheckoutRequest.builder().addressId(3L).build();

        cartService.checkout(userId, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order order = captor.getValue();
        assertEquals(0, order.getTotalPrice().compareTo(BigDecimal.valueOf(2000)));
        assertEquals(0, order.getFinalPrice().compareTo(BigDecimal.valueOf(2000)));
        assertNull(order.getPromoDiscount());
        verify(discountService, never()).redeemPromotion(any(), anyLong());
    }

    @Test
    void checkoutAppliesValidPromoCodeOnDiscountedAmount() {
        CheckoutRequest request = CheckoutRequest.builder().addressId(3L).promoCode("SUMMER25").build();

        PromotionDTO promotion = PromotionDTO.builder()
                .id(1L)
                .code("SUMMER25")
                .discountType("PERCENTAGE")
                .value(BigDecimal.TEN)
                .active(true)
                .build();
        when(discountService.redeemPromotion("SUMMER25", userId)).thenReturn(promotion);

        cartService.checkout(userId, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order order = captor.getValue();
        assertEquals(0, order.getPromoDiscount().compareTo(BigDecimal.valueOf(200)));
        assertEquals(0, order.getFinalPrice().compareTo(BigDecimal.valueOf(1800)));
    }

    @Test
    void checkoutRejectsInvalidPromoCodeAndDoesNotCreateOrder() {
        CheckoutRequest request = CheckoutRequest.builder().addressId(3L).promoCode("BAD").build();

        when(discountService.redeemPromotion("BAD", userId))
                .thenThrow(new BadRequestException("Promo code has expired"));

        assertThrows(BadRequestException.class, () -> cartService.checkout(userId, request));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void addToCartSnapshotsAutomaticallyDiscountedPrice() {
        // Aktivan discount (GLOBAL ili CATEGORY — lookup vraca najspecificniji)
        // se automatski ugradjuje u unit cenu, bez ikakvog koda od strane kupca.
        com.gentlemanstore.discount.model.Discount discount = com.gentlemanstore.discount.model.Discount.builder()
                .id(1L)
                .discountType(com.gentlemanstore.discount.model.DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(20))
                .scope(com.gentlemanstore.discount.model.DiscountScope.GLOBAL)
                .validFrom(java.time.LocalDateTime.now().minusDays(1))
                .validTo(java.time.LocalDateTime.now().plusDays(1))
                .deleted(false)
                .build();

        com.gentlemanstore.product.model.Category category =
                com.gentlemanstore.product.model.Category.builder().id(2L).name("Suits").build();
        Product product = Product.builder().id(5L).name("Suit")
                .price(BigDecimal.valueOf(1000)).category(category).build();
        ProductSize size = ProductSize.builder().id(7L).size("L").quantity(5).product(product).build();

        when(productRepository.findByIdAndDeletedFalse(5L)).thenReturn(Optional.of(product));
        when(productSizeRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(size));
        when(discountRepository.findActiveDiscountForProduct(org.mockito.ArgumentMatchers.eq(2L), any()))
                .thenReturn(Optional.of(discount));
        when(cartMapper.toDTO(any(Cart.class))).thenReturn(new com.gentlemanstore.cart.dto.CartDTO());

        cartService.addToCart(userId, new com.gentlemanstore.cart.dto.AddToCartRequest(5L, 7L, 1));

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getUnitPrice().compareTo(BigDecimal.valueOf(800)));
    }

    @Test
    void fixedPromoCannotDropPriceBelowZero() {
        CheckoutRequest request = CheckoutRequest.builder().addressId(3L).promoCode("BIGFIX").build();

        PromotionDTO promotion = PromotionDTO.builder()
                .id(2L)
                .code("BIGFIX")
                .discountType("FIXED")
                .value(BigDecimal.valueOf(999999))
                .active(true)
                .build();
        when(discountService.redeemPromotion("BIGFIX", userId)).thenReturn(promotion);

        cartService.checkout(userId, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order order = captor.getValue();
        assertEquals(0, order.getFinalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getPromoDiscount().compareTo(BigDecimal.valueOf(2000)));
    }
}
