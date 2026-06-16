package com.gentlemanstore.product.mapper;

import com.gentlemanstore.product.dto.ProductDTO;
import com.gentlemanstore.product.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "sizes", expression = "java(product.getSizes().stream().map(s -> com.gentlemanstore.product.dto.ProductSizeDTO.builder().id(s.getId()).size(s.getSize()).quantity(s.getQuantity()).build()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "imageUrls", expression = "java(product.getImages().stream().map(i -> i.getImageUrl()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "tags", expression = "java(product.getTags().stream().map(t -> t.getName()).collect(java.util.stream.Collectors.toList()))")

    ProductDTO toDTO(Product product);
}
