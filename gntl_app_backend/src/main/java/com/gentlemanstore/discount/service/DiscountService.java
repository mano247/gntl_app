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
import com.gentlemanstore.notification.model.NotificationType;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.product.model.Category;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Discounts = automatski popusti (GLOBAL ili CATEGORY scope), bez koda.
 * Promotions = promo kodovi koje kupac unosi na checkout-u; jednokratni po kupcu.
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    // Bez lako zamenljivih znakova (0/O, 1/I/L).
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_GENERATION_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final DiscountRepository discountRepository;
    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final UserRepository userRepository;
    private final DiscountMapper mapper;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    // ---------- Discounts (automatski popusti) ----------

    @Transactional(readOnly = true)
    public List<DiscountDTO> getAllDiscounts(){
        return discountRepository.findAllByDeletedFalse()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public DiscountDTO createDiscount(CreateDiscountRequest request){
        DiscountType discountType = parseDiscountType(request.getDiscountType());
        validateValue(discountType, request.getValue());
        validateDateRange(request.getValidFrom(), request.getValidTo());

        DiscountScope scope;
        try {
            scope = DiscountScope.valueOf(request.getScope());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid discount scope: " + request.getScope());
        }

        Category category = null;
        if (scope == DiscountScope.CATEGORY) {
            if (request.getCategoryId() == null) {
                throw new BadRequestException("Category is required for CATEGORY scope");
            }
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Discount discount = Discount.builder()
                .discountType(discountType)
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .scope(scope)
                .category(category)
                .deleted(false)
                .build();

        discountRepository.save(discount);

        String title = "New Discount Available!";
        String target = scope == DiscountScope.CATEGORY ? category.getName() : "all products";
        String message = String.format("%.0f%s off on %s from %s to %s",
                discount.getValue(),
                discount.getDiscountType() == DiscountType.PERCENTAGE ? "%" : " RSD",
                target,
                discount.getValidFrom().toLocalDate(),
                discount.getValidTo().toLocalDate());
        notificationService.createNotificationForAllCustomers(title, message, NotificationType.DISCOUNT);

        return mapper.toDTO(discount);
    }

    @Transactional()
    public void deleteDiscount(Long id){
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        discount.setDeleted(true);
        discountRepository.save(discount);
    }

    // ---------- Promotions (promo kodovi) ----------

    @Transactional(readOnly = true)
    public List<PromotionDTO> getAllPromotions(){
        return promotionRepository.findAllByDeletedFalse()
                .stream()
                .map(mapper::toPromotionDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromotionDTO> getActivePromotions(){
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAllByActiveTrueAndValidFromBeforeAndValidToAfterAndDeletedFalse(now, now)
                .stream()
                .map(mapper::toPromotionDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public PromotionDTO createPromotion(CreatePromotionRequest request){
        DiscountType discountType = parseDiscountType(request.getDiscountType());
        validateValue(discountType, request.getValue());
        validateDateRange(request.getValidFrom(), request.getValidTo());

        String code;
        if (request.getCode() == null || request.getCode().isBlank()) {
            code = generateUniqueCode();
        } else {
            code = request.getCode().trim().toUpperCase();
            if (promotionRepository.existsByCode(code)) {
                throw new BadRequestException("Promo code already exists: " + code);
            }
        }

        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .code(code)
                .discountType(discountType)
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .active(true)
                .deleted(false)
                .build();

        promotionRepository.save(promotion);

        String title = "New Promo Code!";
        String message = String.format("Use code %s for %.0f%s off from %s to %s",
                promotion.getCode(),
                promotion.getValue(),
                promotion.getDiscountType() == DiscountType.PERCENTAGE ? "%" : " RSD",
                promotion.getValidFrom().toLocalDate(),
                promotion.getValidTo().toLocalDate());
        notificationService.createNotificationForAllCustomers(title, message, NotificationType.DISCOUNT);

        return mapper.toPromotionDTO(promotion);
    }

    @Transactional()
    public void deletePromotion(Long id){
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        promotion.setDeleted(true);
        promotionRepository.save(promotion);
    }

    /**
     * Validira promo kod za datog korisnika bez markiranja kao iskoriscen —
     * to radi {@link #redeemPromotion} u okviru checkout transakcije.
     */
    @Transactional(readOnly = true)
    public PromotionDTO validatePromoCode(String code, Long userId) {
        Promotion promotion = findValidPromotion(code, userId);
        return mapper.toPromotionDTO(promotion);
    }

    /**
     * Trajno evidentira koriscenje promocije. Parcijalni unique index na
     * user_promotions(user_id, promotion_id) WHERE deleted = false garantuje
     * da paralelni zahtevi ne mogu dovesti do dvostrukog koriscenja —
     * drugi upis pada na constraint-u i cela checkout transakcija se rollback-uje.
     */
    @Transactional
    public PromotionDTO redeemPromotion(String code, Long userId) {
        Promotion promotion = findValidPromotion(code, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserPromotion userPromotion = UserPromotion.builder()
                .user(user)
                .promotion(promotion)
                .usedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        try {
            userPromotionRepository.saveAndFlush(userPromotion);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Promo code already used");
        }

        return mapper.toPromotionDTO(promotion);
    }

    private Promotion findValidPromotion(String code, Long userId) {
        String normalized = code == null ? "" : code.trim().toUpperCase();
        Promotion promotion = promotionRepository.findByCodeAndDeletedFalse(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        if (!promotion.isActive()) {
            throw new BadRequestException("Promo code is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promotion.getValidFrom()) || now.isAfter(promotion.getValidTo())) {
            throw new BadRequestException("Promo code has expired");
        }

        validateValue(promotion.getDiscountType(), promotion.getValue());

        if (userPromotionRepository.existsByUserIdAndPromotionIdAndDeletedFalse(userId, promotion.getId())) {
            throw new BadRequestException("Promo code already used");
        }

        return promotion;
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_ATTEMPTS; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!promotionRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BadRequestException("Could not generate a unique promo code, please try again");
    }

    private DiscountType parseDiscountType(String raw) {
        try {
            return DiscountType.valueOf(raw);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Invalid discount type: " + raw);
        }
    }

    private void validateValue(DiscountType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Discount value must be greater than 0");
        }
        if (type == DiscountType.PERCENTAGE && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequestException("Percentage discount cannot exceed 100");
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("Valid to must be after valid from");
        }
    }
}
