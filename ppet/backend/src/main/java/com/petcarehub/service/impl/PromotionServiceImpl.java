package com.petcarehub.service.impl;

import com.petcarehub.dto.promotion.PromotionDTO;
import com.petcarehub.dto.promotion.PromotionCreateRequest;
import com.petcarehub.entity.Promotion;
import com.petcarehub.repository.PromotionRepository;
import com.petcarehub.service.PromotionService;
import com.petcarehub.exception.ResourceNotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public PromotionDTO createPromotion(PromotionCreateRequest request) {
        Promotion promotion = new Promotion();
        BeanUtils.copyProperties(request, promotion);
        promotion.setCurrentUses(0);
        promotion = promotionRepository.save(promotion);
        return mapToDTO(promotion);
    }

    @Override
    public PromotionDTO getPromotion(Long id) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id " + id));
        return mapToDTO(promotion);
    }

    @Override
    public List<PromotionDTO> getAllPromotions() {
        return promotionRepository.findAll().stream()
            .map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public PromotionDTO updatePromotion(Long id, PromotionCreateRequest request) {
        Promotion promotion = promotionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Promotion not found with id " + id));
        BeanUtils.copyProperties(request, promotion);
        promotion = promotionRepository.save(promotion);
        return mapToDTO(promotion);
    }

    @Override
    public void deletePromotion(Long id) {
        if (!promotionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Promotion not found with id " + id);
        }
        promotionRepository.deleteById(id);
    }

    @Override
    public boolean validatePromotion(String code) {
        Promotion promotion = promotionRepository.findByCode(code).orElse(null);

        if (promotion == null || !Boolean.TRUE.equals(promotion.getActive())) {
            return false;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (promotion.getStartDate() != null && now.isBefore(promotion.getStartDate())) {
            return false;
        }
        
        if (promotion.getEndDate() != null && now.isAfter(promotion.getEndDate())) {
            return false;
        }
        
        if (promotion.getMaxUses() != null && promotion.getCurrentUses() != null &&
            promotion.getCurrentUses() >= promotion.getMaxUses()) {
            return false;
        }
        
        return true;
    }
    
    private PromotionDTO mapToDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        BeanUtils.copyProperties(promotion, dto);
        return dto;
    }
}

