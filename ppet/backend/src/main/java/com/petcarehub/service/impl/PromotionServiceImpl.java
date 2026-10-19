package com.petcarehub.service.impl;

import com.petcarehub.dto.promotion.PromotionDTO;
import com.petcarehub.dto.promotion.PromotionCreateRequest;
import com.petcarehub.entity.Promotion;
import com.petcarehub.repository.PromotionRepository;
import com.petcarehub.service.PromotionService;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.enums.DiscountType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public PromotionDTO createPromotion(PromotionCreateRequest request) {
        Promotion promotion = new Promotion();
        applyRequest(request, promotion);
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
        applyRequest(request, promotion);
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
    
    private void applyRequest(PromotionCreateRequest request, Promotion promotion) {
        if (request.getDiscountType() == null || request.getDiscountValue() == null || request.getDiscountValue().signum() <= 0
                || (request.getDiscountType() == DiscountType.PERCENTAGE && request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0))
            throw new BadRequestException("Discount must be positive; percentage must not exceed 100");
        if (request.getStartDate() != null && request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate()))
            throw new BadRequestException("Promotion end date must not precede start date");
        if (request.getMaxUses() != null && request.getMaxUses() <= 0)
            throw new BadRequestException("Maximum uses must be positive");
        promotion.setCode(request.getCode()); promotion.setDescription(request.getDescription());
        promotion.setDiscountType(request.getDiscountType()); promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMinOrderAmount(request.getMinOrderAmount()); promotion.setMaxUses(request.getMaxUses());
        // Omitting active retains the legacy default (enabled); usage counters remain server-owned.
        promotion.setActive(request.getActive() == null ? true : request.getActive());
        promotion.setStartDate(request.getStartDate()); promotion.setEndDate(request.getEndDate());
    }

    private PromotionDTO mapToDTO(Promotion promotion) {
        PromotionDTO dto = new PromotionDTO();
        dto.setId(promotion.getId()); dto.setCode(promotion.getCode()); dto.setDescription(promotion.getDescription());
        dto.setDiscountType(promotion.getDiscountType()); dto.setDiscountValue(promotion.getDiscountValue());
        dto.setMinOrderAmount(promotion.getMinOrderAmount()); dto.setMaxUses(promotion.getMaxUses());
        dto.setCurrentUses(promotion.getCurrentUses()); dto.setActive(promotion.getActive());
        dto.setStartDate(promotion.getStartDate()); dto.setEndDate(promotion.getEndDate());
        return dto;
    }
}

