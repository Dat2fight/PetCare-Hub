package com.petcarehub.service.impl;

import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.product.ProductDTO;
import com.petcarehub.dto.product.ProductUpdateRequest;
import com.petcarehub.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public Page<ProductDTO> getAllProducts(Long categoryId, Boolean active, Pageable pageable) {
        // TODO: implement logic
        return Page.empty();
    }

    @Override
    public ProductDTO getProductById(Long id) {
        // TODO: implement logic
        return null;
    }

    @Override
    public ProductDTO createProduct(ProductCreateRequest request) {
        // TODO: implement logic
        return null;
    }

    @Override
    public ProductDTO updateProduct(Long id, ProductUpdateRequest request) {
        // TODO: implement logic
        return null;
    }

    @Override
    public void deleteProduct(Long id) {
        // TODO: implement logic
    }
}
