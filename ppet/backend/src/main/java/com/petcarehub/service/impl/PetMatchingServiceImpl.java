package com.petcarehub.service.impl;

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
import com.petcarehub.strategy.matching.MatchingProfile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class PetMatchingServiceImpl implements PetMatchingService {

    private final List<PetMatchingStrategy> strategies;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PetMatchingQuizRepository quizRepository;
    private final PetMatchingResultRepository resultRepository;

    private final java.time.Clock clock;

    public PetMatchingServiceImpl(List<PetMatchingStrategy> strategies, PetRepository petRepository, UserRepository userRepository, PetMatchingQuizRepository quizRepository, PetMatchingResultRepository resultRepository, java.time.Clock clock) {
        this.clock = clock;
        this.strategies = strategies;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public List<MatchResultDTO> matchPets(Long userId, QuizSubmitRequest request) {
        PetMatchingQuiz quiz = new PetMatchingQuiz();
        quiz.setHouseSize(request.getHouseSize());
        quiz.setLivingEnvironment(request.getLivingEnvironment());
        quiz.setHasChildren(request.getPresenceOfChildren());
        quiz.setActivityLevel(request.getActivityLevel());
        quiz.setDailyCareHours(java.math.BigDecimal.valueOf(request.getAvailableCareTime())); // API unit: hours per day.
        quiz.setCustomer(userRepository.findById(userId).orElseThrow(() -> new com.petcarehub.exception.ResourceNotFoundException("Không tìm thấy tài khoản")));

        List<Pet> availablePets = petRepository.findByOwnerIsNullAndAvailabilityStatus(com.petcarehub.enums.AvailabilityStatus.AVAILABLE, org.springframework.data.domain.Pageable.unpaged()).getContent();
        List<ScoredPet> scored = new ArrayList<>();

        for (Pet pet : availablePets) {
            if (!MatchingProfile.complete(pet) || pet.getReservedOrder() != null) continue;
            double totalScore = 0;
            StringBuilder explanation = new StringBuilder();

            for (PetMatchingStrategy strategy : strategies) {
                totalScore += strategy.calculateMatchScore(quiz, pet, explanation);
            }

            scored.add(new ScoredPet(pet, totalScore, explanation.toString(), MatchingProfile.unmetCriteria(quiz, pet)));
        }
        // Only relax lifestyle requirements when no fully eligible pet exists anywhere in the catalog.
        boolean fallback = !scored.isEmpty() && scored.stream().noneMatch(item -> item.unmetCriteria().isEmpty());
        if (!fallback) scored.removeIf(item -> !item.unmetCriteria().isEmpty());
        // Stable ties make stored ranks and the returned top ten reproducible.
        scored.sort(Comparator.comparingDouble(ScoredPet::score).reversed().thenComparing(s -> s.pet().getId()));
        quiz = quizRepository.save(quiz);
        List<PetMatchingResult> persisted = new ArrayList<>();
        List<MatchResultDTO> results = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            ScoredPet item = scored.get(i);
            String explanation = (fallback ? "Gợi ý gần nhất, chưa đáp ứng đầy đủ điều kiện của bạn.\n" : "") + item.explanation();
            PetMatchingResult result = new PetMatchingResult();
            result.setQuiz(quiz); result.setPet(item.pet()); result.setOverallScore((int) item.score());
            result.setExplanation(explanation); result.setRank(i + 1);
            persisted.add(result);
            if (i < 10) results.add(new MatchResultDTO(com.petcarehub.dto.pet.PublicPetDTO.from(com.petcarehub.mapper.PetMapper.toDTO(item.pet(), clock)), item.score(), explanation, fallback, item.unmetCriteria()));
        }
        resultRepository.saveAll(persisted);
        return results;
    }
    private record ScoredPet(Pet pet, double score, String explanation, List<String> unmetCriteria) {}
}



