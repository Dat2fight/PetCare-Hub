package com.petcarehub.service;

import com.petcarehub.dto.promotion.PromotionDTO;
import com.petcarehub.dto.promotion.PromotionCreateRequest;
import java.util.List;

public interface PromotionService {
    PromotionDTO createPromotion(PromotionCreateRequest request);
    PromotionDTO getPromotion(Long id);
    List<PromotionDTO> getAllPromotions();
    PromotionDTO updatePromotion(Long id, PromotionCreateRequest request);
    void deletePromotion(Long id);
    boolean validatePromotion(String code);
}
