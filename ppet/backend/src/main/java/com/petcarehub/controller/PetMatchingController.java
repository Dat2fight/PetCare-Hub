package com.petcarehub.controller;

import com.petcarehub.dto.matching.MatchResultDTO;
import com.petcarehub.dto.matching.QuizSubmitRequest;
import com.petcarehub.service.PetMatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pet-matching")
public class PetMatchingController {

    private final PetMatchingService petMatchingService;

    public PetMatchingController(PetMatchingService petMatchingService) {
        this.petMatchingService = petMatchingService;
    }

    @PostMapping({"/submit", "/submit/{userId}"})
    public ResponseEntity<List<MatchResultDTO>> submitQuiz(@org.springframework.security.core.annotation.AuthenticationPrincipal com.petcarehub.security.UserDetailsImpl principal, @jakarta.validation.Valid @RequestBody QuizSubmitRequest request) {
        if (principal == null) throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        List<MatchResultDTO> results = petMatchingService.matchPets(principal.getId(), request);
        return ResponseEntity.ok(results);
    }
}
