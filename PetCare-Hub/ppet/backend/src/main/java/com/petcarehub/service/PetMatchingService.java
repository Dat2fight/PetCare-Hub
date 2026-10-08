package com.petcarehub.service;

import com.petcarehub.dto.matching.MatchResultDTO;
import com.petcarehub.dto.matching.QuizSubmitRequest;

import java.util.List;

public interface PetMatchingService {
    List<MatchResultDTO> matchPets(Long userId, QuizSubmitRequest request);
}
