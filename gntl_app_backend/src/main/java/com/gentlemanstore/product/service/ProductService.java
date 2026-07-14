package com.gentlemanstore.product.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.discount.repository.DiscountRepository;
import com.gentlemanstore.product.dto.CategoryDTO;
import com.gentlemanstore.product.dto.CreateProductRequest;
import com.gentlemanstore.product.dto.ProductDTO;
import com.gentlemanstore.product.dto.SizeRequest;
import com.gentlemanstore.product.mapper.ProductMapper;
import com.gentlemanstore.product.model.*;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;
    private final ProductMapper mapper;
    private final CategoryRepository categoryRepository;
    private final DiscountRepository discountRepository;

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(String category, String search, String status,
                                           boolean isStaff, Pageable pageable) {
        String normalizedStatus = status == null ? "ACTIVE" : status.trim().toUpperCase();
        boolean includeActive;
        boolean includeDeleted;
        switch (normalizedStatus) {
            case "ACTIVE" -> { includeActive = true; includeDeleted = false; }
            case "DELETED" -> { includeActive = false; includeDeleted = true; }
            case "ALL" -> { includeActive = true; includeDeleted = true; }
            default -> throw new BadRequestException("Invalid product status filter: " + status);
        }
        // Obrisane proizvode vidi samo staff (employee/admin panel) - customer
        // katalog uvek prikazuje iskljucivo aktivne.
        if (includeDeleted && !isStaff) {
            throw new BadRequestException("Invalid product status filter: " + status);
        }

        Page<Long> ids = repo.findIdsByFilters(category, search, includeActive, includeDeleted, pageable);
        List<Product> products = repo.findAllByIdInWithDetails(ids.getContent());
        LocalDateTime now = LocalDateTime.now();
        List<ProductDTO> dtos = products.stream().map(product -> {
            ProductDTO dto = mapper.toDTO(product);
            applyActiveDiscount(dto, product, now);
            return dto;
        }).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, ids.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductDTO getProduct(Long id, boolean isStaff){
        // Staff sme da pregleda i detalje obrisanog proizvoda (DELETED prikaz);
        // za customera obrisan proizvod ostaje 404.
        Product product = (isStaff ? repo.findById(id) : repo.findByIdAndDeletedFalse(id))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductDTO dto = mapper.toDTO(product);
        applyActiveDiscount(dto, product, LocalDateTime.now());
        return dto;
    }

    // discountPercentage u DTO je procenat — FIXED popusti se ne prikazuju kao badge
    // (primenjuju se na cenu tek pri dodavanju u korpu).
    private void applyActiveDiscount(ProductDTO dto, Product product, LocalDateTime now) {
        discountRepository.findActiveDiscountForProduct(product.getCategory().getId(), now)
                .filter(discount -> discount.getDiscountType() == com.gentlemanstore.discount.model.DiscountType.PERCENTAGE)
                .ifPresent(discount -> dto.setDiscountPercentage(discount.getValue()));
    }

    @Transactional()
    public void deleteProduct(Long id){
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setDeleted(true);

        repo.save(product);
    }

    /**
     * Vraca soft-obrisan proizvod u katalog - isti entitet (SKU, opis, slike,
     * tagovi, velicine, kategorija i sve relacije ostaju netaknuti), samo se
     * skida deleted flag. Ne kreira se nov proizvod.
     */
    @Transactional
    public ProductDTO restoreProduct(Long id) {
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isDeleted()) {
            throw new BadRequestException("Product is not deleted");
        }

        product.setDeleted(false);
        repo.save(product);

        ProductDTO dto = mapper.toDTO(product);
        applyActiveDiscount(dto, product, LocalDateTime.now());
        return dto;
    }

    @Transactional()
    public ProductDTO createProduct(CreateProductRequest request) {
        if (repo.existsBySku(request.getSku())) {
            throw new BadRequestException("SKU already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(category)
                .build();

        if (request.getSizes() != null) {
            List<ProductSize> sizes = request.getSizes().stream()
                    .map(s -> ProductSize.builder()
                            .size(s.getSize())
                            .quantity(s.getQuantity())
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.getSizes().addAll(sizes);
        }

        if (request.getImageUrls() != null) {
            List<ProductImage> images = request.getImageUrls().stream()
                    .map(url -> ProductImage.builder()
                            .imageUrl(url)
                            .product(product)
                            .build())
                    .collect(Collectors.toList());
            product.getImages().addAll(images);
        }

        if (request.getTags() != null) {
            List<Tag> tags = request.getTags().stream()
                    .map(tagName -> Tag.builder()
                            .name(tagName)
                            .build())
                    .collect(Collectors.toList());
            product.getTags().addAll(tags);
        }

        repo.save(product);

        return mapper.toDTO(product);
    }

    @Transactional()
    public ProductDTO updateProduct(Long id, CreateProductRequest request){
        Product product = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        if (request.getSizes() != null) {
            syncSizes(product, request.getSizes());
        }

        if (request.getImageUrls() != null) {
            syncImages(product, request.getImageUrls());
        }

        if (request.getTags() != null) {
            product.getTags().clear();
            request.getTags().forEach(tagName ->
                    product.getTags().add(Tag.builder().name(tagName).build()));
        }

        repo.save(product);

        return mapper.toDTO(product);
    }

    // Velicine se sinhronizuju po labelu: postojece se azuriraju (kolicina),
    // izostavljene se soft-delete-uju (cart/order stavke ih i dalje referenciraju),
    // nove se dodaju; ranije obrisana velicina sa istim labelom se reaktivira.
    private void syncSizes(Product product, List<SizeRequest> requestedSizes) {
        Map<String, SizeRequest> requestedByLabel = requestedSizes.stream()
                .collect(Collectors.toMap(
                        s -> s.getSize().trim().toUpperCase(),
                        s -> s,
                        (first, second) -> second));

        for (ProductSize existing : product.getSizes()) {
            SizeRequest match = requestedByLabel.remove(existing.getSize().trim().toUpperCase());
            if (match != null) {
                existing.setQuantity(match.getQuantity());
                existing.setDeleted(false);
            } else if (!existing.isDeleted()) {
                existing.setDeleted(true);
            }
        }

        requestedByLabel.values().forEach(s -> product.getSizes().add(
                ProductSize.builder()
                        .size(s.getSize())
                        .quantity(s.getQuantity())
                        .product(product)
                        .build()));
    }

    private void syncImages(Product product, List<String> requestedUrls) {
        Set<String> remaining = new java.util.LinkedHashSet<>(requestedUrls);

        for (ProductImage existing : product.getImages()) {
            if (remaining.remove(existing.getImageUrl())) {
                existing.setDeleted(false);
            } else if (!existing.isDeleted()) {
                existing.setDeleted(true);
            }
        }

        remaining.forEach(url -> product.getImages().add(
                ProductImage.builder()
                        .imageUrl(url)
                        .product(product)
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAllByDeletedFalse()
                .stream()
                .map(c -> CategoryDTO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
}
