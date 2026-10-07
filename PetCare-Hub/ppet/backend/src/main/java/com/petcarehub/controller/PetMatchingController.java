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

    @PostMapping("/submit/{userId}")
    public ResponseEntity<List<MatchResultDTO>> submitQuiz(@PathVariable Long userId, @RequestBody QuizSubmitRequest request) {
        List<MatchResultDTO> results = petMatchingService.matchPets(userId, request);
        return ResponseEntity.ok(results);
    }
}
