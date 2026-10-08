package com.petcarehub.controller;

import com.petcarehub.dto.product.ProductCategoryDTO;
import com.petcarehub.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public ResponseEntity<List<ProductCategoryDTO>> getAllCategories() {
        return ResponseEntity.ok(productCategoryService.getAllCategories());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDTO> createCategory(@RequestBody ProductCategoryDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDTO> updateCategory(@PathVariable Long id, @RequestBody ProductCategoryDTO request) {
        return ResponseEntity.ok(productCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
