package com.petcarehub.service;

import com.petcarehub.dto.product.ProductCategoryDTO;

import java.util.List;

public interface ProductCategoryService {
    List<ProductCategoryDTO> getAllCategories();
    ProductCategoryDTO getCategoryById(Long id);
    ProductCategoryDTO createCategory(ProductCategoryDTO request);
    ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO request);
    void deleteCategory(Long id);
}
