package com.petcarehub.strategy.matching;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;

public interface PetMatchingStrategy {
    double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation);
}
