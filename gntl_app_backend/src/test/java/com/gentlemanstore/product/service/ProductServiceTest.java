package com.gentlemanstore.product.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.product.dto.CreateProductRequest;
import com.gentlemanstore.product.mapper.ProductMapper;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository repo;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper mapper;

    @InjectMocks
    private ProductService productService;

    @Test
    void shouldThrowExceptionWhenSkuAlreadyExists() {
        // given
        CreateProductRequest request = new CreateProductRequest();
        request.setSku("SKU001");

        when(repo.existsBySku("SKU001")).thenReturn(true);

        // when + then
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(request);
        });
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        // given
        when(repo.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());

        // when + then
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProduct(1L);
        });
    }
}
