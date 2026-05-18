package com.gentlemanstore.discount.mapper;

import com.gentlemanstore.discount.dto.DiscountDTO;
import com.gentlemanstore.discount.dto.PromotionDTO;
import com.gentlemanstore.discount.model.Discount;
import com.gentlemanstore.discount.model.Promotion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiscountMapper {
    @Mapping(source = "discountType", target = "discountType")
    DiscountDTO toDTO(Discount discount);

    @Mapping(source = "discount.code", target = "discountCode")
    PromotionDTO toPromotionDTO(Promotion promotion);
}
