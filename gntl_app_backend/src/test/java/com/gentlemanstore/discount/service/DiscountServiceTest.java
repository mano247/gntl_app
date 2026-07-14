package com.gentlemanstore.discount.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.discount.dto.CreateDiscountRequest;
import com.gentlemanstore.discount.dto.CreatePromotionRequest;
import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.mapper.DiscountMapper;
import com.gentlemanstore.discount.model.*;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.discount.repository.PromotionRepository;
import com.gentlemanstore.discount.repository.UserPromotionRepository;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private UserPromotionRepository userPromotionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DiscountMapper mapper;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DiscountService discountService;

    private Promotion activePromotion(Long id, String code) {
        return Promotion.builder()
                .id(id)
                .name("Test promo")
                .description("desc")
                .code(code)
                .discountType(DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusDays(1))
                .active(true)
                .deleted(false)
                .build();
    }

    // ---------- Discounts ----------

    @Test
    void shouldCreateGlobalDiscountWithoutCode() {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .discountType("PERCENTAGE")
                .value(BigDecimal.valueOf(20))
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .scope("GLOBAL")
                .build();

        when(mapper.toDTO(any(Discount.class))).thenReturn(new DiscountDTO());

        discountService.createDiscount(request);

        ArgumentCaptor<Discount> captor = ArgumentCaptor.forClass(Discount.class);
        verify(discountRepository).save(captor.capture());
        assertEquals(DiscountScope.GLOBAL, captor.getValue().getScope());
        assertNull(captor.getValue().getCategory());
    }

    @Test
    void shouldRejectCategoryScopeWithoutCategoryId() {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .discountType("PERCENTAGE")
                .value(BigDecimal.valueOf(20))
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .scope("CATEGORY")
                .build();

        assertThrows(BadRequestException.class, () -> discountService.createDiscount(request));
        verify(discountRepository, never()).save(any());
    }

    @Test
    void shouldRejectPercentageDiscountOver100() {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .discountType("PERCENTAGE")
                .value(BigDecimal.valueOf(150))
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .scope("GLOBAL")
                .build();

        assertThrows(BadRequestException.class, () -> discountService.createDiscount(request));
    }

    @Test
    void shouldRejectInvalidScope() {
        CreateDiscountRequest request = CreateDiscountRequest.builder()
                .discountType("PERCENTAGE")
                .value(BigDecimal.valueOf(10))
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .scope("PRODUCT")
                .build();

        assertThrows(BadRequestException.class, () -> discountService.createDiscount(request));
    }

    // ---------- Promotions ----------

    @Test
    void shouldGenerateReadableUniqueCodeWhenNotProvided() {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .name("Summer")
                .description("Summer promo")
                .discountType("PERCENTAGE")
                .value(BigDecimal.TEN)
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .build();

        when(promotionRepository.existsByCode(anyString())).thenReturn(false);
        when(mapper.toPromotionDTO(any(Promotion.class))).thenReturn(new PromotionDTO());

        discountService.createPromotion(request);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        String code = captor.getValue().getCode();
        assertNotNull(code);
        assertEquals(8, code.length());
        // bez lako zamenljivih znakova
        assertFalse(code.contains("0"));
        assertFalse(code.contains("O"));
        assertFalse(code.contains("1"));
        assertFalse(code.contains("I"));
        assertFalse(code.contains("L"));
    }

    @Test
    void shouldUppercaseAndUseProvidedCode() {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .name("Summer")
                .description("Summer promo")
                .code("summer25")
                .discountType("FIXED")
                .value(BigDecimal.valueOf(500))
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .build();

        when(promotionRepository.existsByCode("SUMMER25")).thenReturn(false);
        when(mapper.toPromotionDTO(any(Promotion.class))).thenReturn(new PromotionDTO());

        discountService.createPromotion(request);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertEquals("SUMMER25", captor.getValue().getCode());
    }

    @Test
    void shouldRejectDuplicatePromoCode() {
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .name("Summer")
                .description("Summer promo")
                .code("SUMMER25")
                .discountType("PERCENTAGE")
                .value(BigDecimal.TEN)
                .validFrom(LocalDateTime.now())
                .validTo(LocalDateTime.now().plusDays(7))
                .build();

        when(promotionRepository.existsByCode("SUMMER25")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> discountService.createPromotion(request));
        verify(promotionRepository, never()).save(any());
    }

    // ---------- Validacija promo koda ----------

    @Test
    void shouldRejectUnknownPromoCode() {
        when(promotionRepository.findByCodeAndDeletedFalse("MISSING")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> discountService.validatePromoCode("MISSING", 1L));
    }

    @Test
    void shouldRejectInactivePromotion() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        promotion.setActive(false);
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));

        assertThrows(BadRequestException.class,
                () -> discountService.validatePromoCode("CODE1234", 1L));
    }

    @Test
    void shouldRejectExpiredPromotion() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        promotion.setValidFrom(LocalDateTime.now().minusDays(10));
        promotion.setValidTo(LocalDateTime.now().minusDays(1));
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));

        assertThrows(BadRequestException.class,
                () -> discountService.validatePromoCode("CODE1234", 1L));
    }

    @Test
    void shouldRejectAlreadyUsedPromoCodeForSameCustomer() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));
        when(userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(1L, 1L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> discountService.validatePromoCode("CODE1234", 1L));
    }

    @Test
    void shouldAllowSameCodeForDifferentCustomer() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));
        when(userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(2L, 1L)).thenReturn(false);
        when(mapper.toPromotionDTO(promotion)).thenReturn(new PromotionDTO());

        assertDoesNotThrow(() -> discountService.validatePromoCode("CODE1234", 2L));
    }

    @Test
    void shouldNormalizeCodeCaseAndWhitespaceOnValidation() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));
        when(userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(1L, 1L)).thenReturn(false);
        when(mapper.toPromotionDTO(promotion)).thenReturn(new PromotionDTO());

        assertDoesNotThrow(() -> discountService.validatePromoCode("  code1234 ", 1L));
    }

    // ---------- Redeem (trajna evidencija + zastita od paralelnog koriscenja) ----------

    @Test
    void shouldPersistUsageOnRedeem() {
        Promotion promotion = activePromotion(1L, "CODE1234");
        User user = new User();
        user.setId(1L);
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));
        when(userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(mapper.toPromotionDTO(promotion)).thenReturn(new PromotionDTO());

        discountService.redeemPromotion("CODE1234", 1L);

        ArgumentCaptor<UserPromotion> captor = ArgumentCaptor.forClass(UserPromotion.class);
        verify(userPromotionRepository).saveAndFlush(captor.capture());
        assertEquals(1L, captor.getValue().getUser().getId());
        assertEquals(1L, captor.getValue().getPromotion().getId());
    }

    @Test
    void shouldTranslateConstraintViolationToAlreadyUsed() {
        // Simulacija paralelnog zahteva: provera prolazi, ali unique index u bazi
        // odbija drugi upis — servis to prevodi u BadRequest ("already used").
        Promotion promotion = activePromotion(1L, "CODE1234");
        User user = new User();
        user.setId(1L);
        when(promotionRepository.findByCodeAndDeletedFalse("CODE1234")).thenReturn(Optional.of(promotion));
        when(userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(1L, 1L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userPromotionRepository.saveAndFlush(any(UserPromotion.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> discountService.redeemPromotion("CODE1234", 1L));
        assertTrue(ex.getMessage().contains("already used"));
    }
}
