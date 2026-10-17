package com.petcarehub.controller;

import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.dto.product.ProductDTO;
import com.petcarehub.dto.product.ProductUpdateRequest;
import com.petcarehub.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<ProductDTO>> getAllProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        log.info("Fetching products - categoryId: {}, active: {}, pageable: {}", categoryId, active, pageable);
        return ResponseEntity.ok(productService.searchProducts(categoryId, true, keyword, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        log.info("Fetching product by id: {}", id);
        ProductDTO product = productService.getProductById(id);
        if (!Boolean.TRUE.equals(product.getActive())) throw new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy sản phẩm");
        return ResponseEntity.ok(product);
    }

    @GetMapping("/manage")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF', 'WAREHOUSE_STAFF')")
    public ResponseEntity<Page<ProductDTO>> manageProducts(@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Boolean active, @RequestParam(required=false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(productService.searchProducts(categoryId, active, keyword, pageable));
    }

    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<ProductDTO> createProduct(@jakarta.validation.Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @jakarta.validation.Valid @RequestBody ProductUpdateRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
