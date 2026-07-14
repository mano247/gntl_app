package com.gentlemanstore.product.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.product.dto.CreateProductRequest;
import com.gentlemanstore.product.dto.ProductDTO;
import com.gentlemanstore.product.dto.SizeRequest;
import com.gentlemanstore.product.mapper.ProductMapper;
import com.gentlemanstore.product.model.Category;
import com.gentlemanstore.product.model.Product;
import com.gentlemanstore.product.model.ProductSize;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    @Mock
    private ProductRepository repo;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper mapper;

    @Mock
    private DiscountRepository discountRepository;

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

    @Test
    void updateShouldSyncSizesPricesAndImages() {
        // given — proizvod sa velicinama M (5 kom) i L (3 kom) i jednom slikom
        Category category = Category.builder().id(1L).name("Suits").build();
        Product product = Product.builder()
                .id(1L)
                .sku("SKU001")
                .name("Old name")
                .description("Old desc")
                .price(BigDecimal.valueOf(1000))
                .category(category)
                .build();
        ProductSize sizeM = ProductSize.builder().id(11L).size("M").quantity(5).product(product).build();
        ProductSize sizeL = ProductSize.builder().id(12L).size("L").quantity(3).product(product).build();
        product.getSizes().add(sizeM);
        product.getSizes().add(sizeL);
        com.gentlemanstore.product.model.ProductImage image = com.gentlemanstore.product.model.ProductImage.builder()
                .id(21L).imageUrl("http://old.jpg").product(product).build();
        product.getImages().add(image);

        when(repo.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(product));
        when(mapper.toDTO(any(Product.class))).thenReturn(new ProductDTO());

        // when — nova cena, M dobija novu kolicinu, L se izostavlja, XL je nov;
        // stara slika se menja novom
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU001")
                .name("New name")
                .description("New desc")
                .price(BigDecimal.valueOf(1500))
                .categoryId(1L)
                .sizes(List.of(new SizeRequest("M", 10), new SizeRequest("XL", 2)))
                .imageUrls(List.of("http://new.jpg"))
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        productService.updateProduct(1L, request);

        // then
        assertEquals(BigDecimal.valueOf(1500), product.getPrice());
        assertEquals("New name", product.getName());
        assertEquals(10, sizeM.getQuantity());
        assertFalse(sizeM.isDeleted());
        assertTrue(sizeL.isDeleted(), "izostavljena velicina se soft-delete-uje, ne brise fizicki");
        assertTrue(product.getSizes().stream().anyMatch(s -> s.getSize().equals("XL") && s.getQuantity() == 2));
        assertTrue(image.isDeleted(), "stara slika se soft-delete-uje");
        assertTrue(product.getImages().stream().anyMatch(i -> i.getImageUrl().equals("http://new.jpg") && !i.isDeleted()));
        verify(repo).save(product);
    }

    @Test
    void deleteShouldBeSoftDelete() {
        Product product = Product.builder().id(1L).sku("SKU001").build();
        when(repo.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        assertTrue(product.isDeleted());
        verify(repo).save(product);
    }
}
