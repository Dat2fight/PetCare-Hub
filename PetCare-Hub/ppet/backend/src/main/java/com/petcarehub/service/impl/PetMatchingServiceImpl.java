package com.petcarehub.service.impl;

import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.dto.matching.MatchResultDTO;
import com.petcarehub.dto.matching.QuizSubmitRequest;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import com.petcarehub.entity.PetMatchingResult;
import com.petcarehub.repository.PetMatchingQuizRepository;
import com.petcarehub.repository.PetMatchingResultRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.service.PetMatchingService;
import com.petcarehub.strategy.matching.PetMatchingStrategy;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PetMatchingServiceImpl implements PetMatchingService {

    private final List<PetMatchingStrategy> strategies;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetMatchingQuizRepository quizRepository;
    private final PetMatchingResultRepository resultRepository;

    public PetMatchingServiceImpl(List<PetMatchingStrategy> strategies, PetRepository petRepository, UserRepository userRepository, PetMatchingQuizRepository quizRepository, PetMatchingResultRepository resultRepository) {
        this.strategies = strategies;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public List<MatchResultDTO> matchPets(Long userId, QuizSubmitRequest request) {
        PetMatchingQuiz quiz = new PetMatchingQuiz();
        BeanUtils.copyProperties(request, quiz);
        quiz.setCustomer(userRepository.findById(userId).orElse(null));
        quiz = quizRepository.save(quiz);

        List<Pet> availablePets = petRepository.findByAvailabilityStatus(com.petcarehub.enums.AvailabilityStatus.AVAILABLE, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<MatchResultDTO> results = new ArrayList<>();

        for (Pet pet : availablePets) {
            double totalScore = 0;
            StringBuilder explanation = new StringBuilder();

            for (PetMatchingStrategy strategy : strategies) {
                totalScore += strategy.calculateMatchScore(quiz, pet, explanation);
            }

            PetMatchingResult resultEntity = new PetMatchingResult();
            resultEntity.setQuiz(quiz);
            resultEntity.setPet(pet);
            resultEntity.setOverallScore((int) totalScore);
            resultEntity.setExplanation(explanation.toString());
            resultRepository.save(resultEntity);

            PetDTO petDTO = new PetDTO();
            BeanUtils.copyProperties(pet, petDTO);
            results.add(new MatchResultDTO(petDTO, totalScore, explanation.toString()));
        }

        return results.stream()
                .sorted(Comparator.comparing(MatchResultDTO::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}



