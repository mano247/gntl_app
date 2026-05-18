package com.gentlemanstore.discount.service;

import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.discount.dto.CreateDiscountRequest;
import com.gentlemanstore.discount.dto.CreatePromotionRequest;
import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.mapper.DiscountMapper;
import com.gentlemanstore.discount.model.Discount;
import com.gentlemanstore.discount.model.DiscountType;
import com.gentlemanstore.discount.model.Promotion;
import com.gentlemanstore.discount.model.UserPromotion;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.discount.repository.PromotionRepository;
import com.gentlemanstore.discount.repository.UserPromotionRepository;
import com.gentlemanstore.user.model.User;
import com.gentlemanstore.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public DiscountDTO getDiscount(String code){
        Discount discount = discountRepository.findByCodeAndDeletedFalse(code)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        return mapper.toDTO(discount);
    }

    public List<DiscountDTO> getAllDiscounts(){
        return discountRepository.findAllByDeletedFalse()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public DiscountDTO createDiscount(CreateDiscountRequest request){
        Discount discount = Discount.builder()
                .code(request.getCode())
                .discountType(DiscountType.valueOf(request.getDiscountType()))
                .value(request.getValue())
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .deleted(false)
                .build();

        discountRepository.save(discount);
        return mapper.toDTO(discount);
    }

    public void deleteDiscount(Long id){
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Discount not found"));

        discount.setDeleted(true);
        discountRepository.save(discount);
    }

    public List<PromotionDTO> getActivePromotions(){
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAllByValidFromBeforeAndValidToAfterAndDeletedFalse(now, now)
                .stream()
                .map(mapper::toPromotionDTO)
                .collect(Collectors.toList());
    }

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
}
