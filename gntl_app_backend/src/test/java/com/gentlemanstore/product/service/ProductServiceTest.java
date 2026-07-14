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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
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
            productService.getProduct(1L, false);
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

    // ---------- restore (Employee Products - DELETED prikaz) ----------

    @Test
    void restoreShouldReviveSameEntityWithoutCreatingNew() {
        Category category = Category.builder().id(1L).name("Suits").build();
        Product product = Product.builder()
                .id(1L)
                .sku("SKU001")
                .name("Suit")
                .price(BigDecimal.TEN)
                .category(category)
                .build();
        product.setDeleted(true);
        when(repo.findById(1L)).thenReturn(Optional.of(product));
        when(mapper.toDTO(product)).thenReturn(new ProductDTO());
        when(discountRepository.findActiveDiscountForProduct(any(), any()))
                .thenReturn(Optional.empty());

        productService.restoreProduct(1L);

        assertFalse(product.isDeleted(), "isti entitet se vraca u katalog");
        assertEquals("SKU001", product.getSku(), "SKU i ostali podaci ostaju netaknuti");
        verify(repo).save(product);
    }

    @Test
    void restoreNonDeletedProductThrows400() {
        Product product = Product.builder().id(1L).sku("SKU001").build();
        when(repo.findById(1L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> productService.restoreProduct(1L));
        verify(repo, never()).save(any());
    }

    @Test
    void restoreMissingProductThrows404() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.restoreProduct(99L));
    }

    // ---------- status filter (ACTIVE / DELETED / ALL) ----------

    @Test
    void deletedFilterIsStaffOnly() {
        assertThrows(BadRequestException.class, () ->
                productService.getAllProducts(null, null, "DELETED", false, org.springframework.data.domain.Pageable.unpaged()));
        assertThrows(BadRequestException.class, () ->
                productService.getAllProducts(null, null, "ALL", false, org.springframework.data.domain.Pageable.unpaged()));
    }

    @Test
    void invalidStatusFilterThrows400() {
        assertThrows(BadRequestException.class, () ->
                productService.getAllProducts(null, null, "WHATEVER", true, org.springframework.data.domain.Pageable.unpaged()));
    }

    @Test
    void statusFilterMapsToRepositoryFlags() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.Pageable.unpaged();
        when(repo.findIdsByFilters(any(), any(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        when(repo.findAllByIdInWithDetails(any())).thenReturn(List.of());

        productService.getAllProducts(null, null, null, false, pageable);
        verify(repo).findIdsByFilters(null, null, true, false, pageable);

        productService.getAllProducts(null, null, "DELETED", true, pageable);
        verify(repo).findIdsByFilters(null, null, false, true, pageable);

        productService.getAllProducts(null, null, "ALL", true, pageable);
        verify(repo).findIdsByFilters(null, null, true, true, pageable);
    }

    @Test
    void staffCanReadDeletedProductDetailsButCustomerCannot() {
        Category category = Category.builder().id(1L).name("Suits").build();
        Product deletedProduct = Product.builder().id(1L).sku("SKU001").category(category).build();
        deletedProduct.setDeleted(true);

        when(repo.findById(1L)).thenReturn(Optional.of(deletedProduct));
        when(mapper.toDTO(deletedProduct)).thenReturn(new ProductDTO());
        when(discountRepository.findActiveDiscountForProduct(any(), any()))
                .thenReturn(Optional.empty());

        assertNotNull(productService.getProduct(1L, true), "staff vidi detalje obrisanog proizvoda");

        when(repo.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(1L, false),
                "customer i dalje dobija 404 za obrisan proizvod");
    }
}
