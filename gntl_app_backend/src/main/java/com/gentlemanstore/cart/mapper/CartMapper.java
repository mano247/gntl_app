package com.gentlemanstore.cart.mapper;

import com.gentlemanstore.cart.dto.CartDTO;
import com.gentlemanstore.cart.dto.CartItemDTO;
import com.gentlemanstore.cart.model.Cart;
import com.gentlemanstore.cart.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "productSize.size", target = "size")
    @Mapping(source = "product.price", target = "price")
    CartItemDTO toItemDTO(CartItem cartItem);

    @Mapping(target = "totalPrice", ignore = true)
    CartDTO toDTO(Cart cart);
}
