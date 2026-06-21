package com.gentlemanstore.order.mapper;

import com.gentlemanstore.order.dto.OrderDTO;
import com.gentlemanstore.order.dto.OrderItemDTO;
import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.order.model.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "status", target = "status")
    @Mapping(source = "orderItems", target = "items")
    OrderDTO toDTO(Order order);

    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "totalPrice", target = "price")
    @Mapping(source = "productSize.size", target = "size")
    @Mapping(source = "product.price", target = "originalPrice")
    @Mapping(target = "imageUrl", expression = "java(orderItem.getProduct().getImages().isEmpty() ? null : orderItem.getProduct().getImages().iterator().next().getImageUrl())")
    OrderItemDTO toItemDTO(OrderItem orderItem);
}