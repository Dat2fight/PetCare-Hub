package com.petcarehub.service.impl;

import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.product.ProductDTO;
import com.petcarehub.dto.product.ProductUpdateRequest;
import com.petcarehub.entity.Product;
import com.petcarehub.entity.ProductCategory;
import com.petcarehub.entity.Inventory;
import com.petcarehub.repository.InventoryRepository;
import com.petcarehub.repository.ProductCategoryRepository;
import com.petcarehub.repository.ProductRepository;
import com.petcarehub.service.ProductService;
import jakarta.persistence.EntityNotFoundException;
import com.petcarehub.exception.ResourceNotFoundException;
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
    private final InventoryRepository inventoryRepository;

    private ProductDTO toDTO(Product p) {
        return toDTO(p, inventoryRepository.findByProductId(p.getId()).map(Inventory::getQuantity).orElse(0));
    }

    private ProductDTO toDTO(Product p, int quantity) {
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stockQuantity(quantity)
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
        return searchProducts(categoryId, active, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(Long categoryId, Boolean active, String keyword, Pageable pageable) {
        org.springframework.data.jpa.domain.Specification<Product> filter = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            if (keyword != null && !keyword.isBlank()) predicates.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Page<Product> products = productRepository.findAll(filter, pageable);
        var ids = products.stream().map(Product::getId).toList();
        var quantities = ids.isEmpty() ? java.util.Map.<Long,Integer>of() : inventoryRepository.findByProductIdIn(ids).stream()
                .collect(java.util.stream.Collectors.toMap(i -> i.getProduct().getId(), Inventory::getQuantity));
        return products.map(p -> toDTO(p, quantities.getOrDefault(p.getId(), 0)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
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

        if (request.getStockQuantity() != null) throw new com.petcarehub.exception.BadRequestException("Dùng API nhập, xuất hoặc kiểm kê để thay đổi tồn kho");
        if (request.getCategoryId() != null) product.setCategory(productCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục")));
        if (request.getName() != null)
            product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getPrice() != null)
            product.setPrice(request.getPrice());
        if (request.getActive() != null)
            product.setActive(request.getActive());
        if (request.getImageUrl() != null)
            product.setImageUrl(request.getImageUrl());
        if (request.getSku() != null)
            product.setSku(request.getSku());

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
