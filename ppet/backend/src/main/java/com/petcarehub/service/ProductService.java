package com.petcarehub.service;

import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.product.ProductDTO;
import com.petcarehub.dto.product.ProductUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    Page<ProductDTO> getAllProducts(Long categoryId, Boolean active, Pageable pageable);
    ProductDTO getProductById(Long id);
    ProductDTO createProduct(ProductCreateRequest request);
    ProductDTO updateProduct(Long id, ProductUpdateRequest request);
    void deleteProduct(Long id);
}
