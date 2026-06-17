package com.gentlemanstore.user.mapper;

import com.gentlemanstore.user.dto.AddressDTO;
import com.gentlemanstore.user.model.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressDTO toDTO(Address address);
}