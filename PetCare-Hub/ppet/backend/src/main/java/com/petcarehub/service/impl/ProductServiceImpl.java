package com.petcarehub.service.impl;

import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.product.ProductDTO;
import com.petcarehub.dto.product.ProductUpdateRequest;
import com.petcarehub.entity.Product;
import com.petcarehub.entity.ProductCategory;
import com.petcarehub.repository.ProductCategoryRepository;
import com.petcarehub.repository.ProductRepository;
import com.petcarehub.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .imageUrl(p.getImageUrl())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .sku(p.getSku())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Long categoryId, Boolean active, Pageable pageable) {
        Page<Product> products;
        if (categoryId != null && active != null && active) {
            products = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable);
        } else if (active != null && active) {
            products = productRepository.findByActiveTrue(pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        return toDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateRequest request) {
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new EntityNotFoundException("Category not found with id: " + request.getCategoryId()));

        String sku = request.getSku();
        if (sku == null || sku.isBlank()) {
            sku = "SKU-" + System.currentTimeMillis();
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .category(category)
                .sku(sku)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));

        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getPrice() != null)
            product.setPrice(request.getPrice());
        if (request.getActive() != null)
            product.setActive(request.getActive());

        return toDTO(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }
}
