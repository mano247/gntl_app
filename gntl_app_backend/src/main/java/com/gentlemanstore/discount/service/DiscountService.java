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
import com.gentlemanstore.discount.repository.UserDiscountRepository;
import com.gentlemanstore.discount.repository.UserPromotionRepository;
import com.gentlemanstore.notification.model.NotificationType;
import com.gentlemanstore.notification.service.NotificationService;
import com.gentlemanstore.product.model.Category;
import com.gentlemanstore.product.model.Product;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;
    private final PromotionRepository promotionRepository;
    private final UserPromotionRepository userPromotionRepository;
    private final UserRepository userRepository;
    private final DiscountMapper mapper;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserDiscountRepository userDiscountRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public DiscountDTO getDiscount(String code){
        Discount discount = discountRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        return mapper.toDTO(discount);
    }

    @Transactional(readOnly = true)
    public List<DiscountDTO> getAllDiscounts(){
        return discountRepository.findAllByDeletedFalse()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public DiscountDTO createDiscount(CreateDiscountRequest request){
        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findByIdAndDeletedFalse(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        }

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        }

        Discount discount = Discount.builder()
                .code(request.getCode())
                .discountType(DiscountType.valueOf(request.getDiscountType()))
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .product(product)
                .category(category)
                .deleted(false)
                .build();

        discountRepository.save(discount);

        String title = "New Discount Available!";
        String message = String.format("New promotion! Code: %s, %.0f%s off from %s to %s",
                discount.getCode(),
                discount.getValue(),
                discount.getDiscountType() == DiscountType.PERCENTAGE ? "%" : " RSD",
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

    @Transactional(readOnly = true)
    public List<PromotionDTO> getActivePromotions(){
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAllByValidFromBeforeAndValidToAfterAndDeletedFalse(now, now)
                .stream()
                .map(mapper::toPromotionDTO)
                .collect(Collectors.toList());
    }

    @Transactional()
    public PromotionDTO createPromotion(CreatePromotionRequest request){
        Discount discount = discountRepository.findById(request.getDiscountId())
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        Promotion promotion = Promotion.builder()
                .name(request.getName())
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .discount(discount)
                .deleted(false)
                .build();

        promotionRepository.save(promotion);
        return mapper.toPromotionDTO(promotion);
    }

    @Transactional()
    public void applyPromotion(Long userId, Long promotionId){
        Promotion promotion = promotionRepository.findById(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserPromotion userPromotion = UserPromotion.builder()
                .user(user)
                .promotion(promotion)
                .usedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        userPromotionRepository.save(userPromotion);
    }

    @Transactional
    public DiscountDTO validateAndUsePromoCode(String code, Long userId) {
        Discount discount = discountRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(discount.getValidFrom()) || now.isAfter(discount.getValidTo())) {
            throw new BadRequestException("Promo code has expired");
        }

        if (userDiscountRepository.existsByUserIdAndDiscountIdAndDeletedFalse(userId, discount.getId())) {
            throw new BadRequestException("Promo code already used");
        }

        return mapper.toDTO(discount);
    }

    @Transactional
    public void markPromoCodeAsUsed(String code, Long userId) {
        Discount discount = discountRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promo code not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserDiscount userDiscount = UserDiscount.builder()
                .user(user)
                .discount(discount)
                .usedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        userDiscountRepository.save(userDiscount);
    }
}
