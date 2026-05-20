package com.gentlemanstore.product.service;

import com.gentlemanstore.common.exception.BadRequestException;
import com.gentlemanstore.common.exception.ResourceNotFoundException;
import com.gentlemanstore.product.dto.CreateProductRequest;
import com.gentlemanstore.product.dto.ProductDTO;
import com.gentlemanstore.product.mapper.ProductMapper;
import com.gentlemanstore.product.model.*;
import com.gentlemanstore.product.repository.CategoryRepository;
import com.gentlemanstore.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;
    private final ProductMapper mapper;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public ProductDTO getProduct(Long id){
        Product product = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapper.toDTO(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return repo.findAllByDeletedFalse(pageable)
                .map(mapper::toDTO);
    }

    @Transactional()
    public void deleteProduct(Long id){
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setDeleted(true);

        repo.save(product);
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
        Product product = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            product.setCategory(category);
        }

        repo.save(product);

        return mapper.toDTO(product);
    }
}
