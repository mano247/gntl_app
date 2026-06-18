package com.gentlemanstore.user.mapper;

import com.gentlemanstore.user.dto.UserDTO;
import com.gentlemanstore.user.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = AddressMapper.class)
public interface UserMapper {
    UserDTO toDTO(User user);
}
