package com.petcarehub.controller;

import com.petcarehub.dto.promotion.PromotionDTO;
import com.petcarehub.dto.promotion.PromotionCreateRequest;
import com.petcarehub.service.PromotionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    @PostMapping
    public ResponseEntity<PromotionDTO> createPromotion(@RequestBody PromotionCreateRequest request) {
        return ResponseEntity.ok(promotionService.createPromotion(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO> getPromotion(@PathVariable Long id) {
        return ResponseEntity.ok(promotionService.getPromotion(id));
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> getAllPromotions() {
        return ResponseEntity.ok(promotionService.getAllPromotions());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionDTO> updatePromotion(@PathVariable Long id, @RequestBody PromotionCreateRequest request) {
        return ResponseEntity.ok(promotionService.updatePromotion(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<Boolean> validatePromotion(@PathVariable String code) {
        return ResponseEntity.ok(promotionService.validatePromotion(code));
    }
}
