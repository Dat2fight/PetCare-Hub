package com.petcarehub.service.impl;

import com.petcarehub.dto.product.ProductCategoryDTO;
import com.petcarehub.service.ProductCategoryService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ProductCategoryServiceImpl implements ProductCategoryService {

    @Override
    public List<ProductCategoryDTO> getAllCategories() {
        // TODO: implement logic
        return Collections.emptyList();
    }

    @Override
    public ProductCategoryDTO getCategoryById(Long id) {
        // TODO: implement logic
        return null;
    }

    @Override
    public ProductCategoryDTO createCategory(ProductCategoryDTO request) {
        // TODO: implement logic
        return null;
    }

    @Override
    public ProductCategoryDTO updateCategory(Long id, ProductCategoryDTO request) {
        // TODO: implement logic
        return null;
    }

    @Override
    public void deleteCategory(Long id) {
        // TODO: implement logic
    }
}
