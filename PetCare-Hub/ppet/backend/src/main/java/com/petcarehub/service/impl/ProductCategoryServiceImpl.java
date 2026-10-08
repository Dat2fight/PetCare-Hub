package com.petcarehub.service.impl;

import com.petcarehub.dto.product.ProductCategoryDTO;
import com.petcarehub.entity.ProductCategory;
import com.petcarehub.repository.ProductCategoryRepository;
import com.petcarehub.service.ProductCategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    private ProductCategoryDTO toDTO(ProductCategory cat) {
        return new ProductCategoryDTO(
                cat.getId(),
                cat.getName(),
                cat.getDescription(),
                cat.getParentCategory() != null ? cat.getParentCategory().getId() : null,
                null // skip children to avoid recursion
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryDTO getCategoryById(Long id) {
        return toDTO(categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id)));
    }

    @Override
    @Transactional
    public ProductCategoryDTO createCategory(ProductCategoryDTO request) {
        ProductCategory cat = new ProductCategory();
        cat.setName(request.getName());
        cat.setDescription(request.getDescription());
        if (request.getParentId() != null) {
            cat.setParentCategory(categoryRepository.findById(request.getParentId()).orElse(null));
        }
        return toDTO(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO request) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        if (request.getName() != null)
            cat.setName(request.getName());
        if (request.getDescription() != null)
            cat.setDescription(request.getDescription());
        return toDTO(categoryRepository.save(cat));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
