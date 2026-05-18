package com.gentlemanstore.loyalty.mapper;

import com.gentlemanstore.loyalty.dto.LoyaltyAccountDTO;
import com.gentlemanstore.loyalty.dto.LoyaltyTransactionDTO;
import com.gentlemanstore.loyalty.model.LoyaltyAccount;
import com.gentlemanstore.loyalty.model.LoyaltyTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LoyaltyMapper {
    @Mapping(source = "loyaltyTier.name", target = "tierName")
    @Mapping(source = "loyaltyTier.discountPercentage", target = "discountPercentage")
    LoyaltyAccountDTO toDTO(LoyaltyAccount account);

    LoyaltyTransactionDTO toTransactionDTO(LoyaltyTransaction transaction);
}
