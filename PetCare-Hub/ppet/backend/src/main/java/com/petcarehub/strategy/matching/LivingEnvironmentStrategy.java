package com.petcarehub.strategy.matching;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class LivingEnvironmentStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        double score = 0;
        BigDecimal petSize = pet.getWeight();
        
        if (quiz.getLivingEnvironment() != null && quiz.getLivingEnvironment().equalsIgnoreCase("APARTMENT")) {
            if (petSize != null && petSize.compareTo(new BigDecimal("20")) > 0) {
                explanation.append("A large pet might not be ideal for an apartment. ");
                score += 5.0;
            } else {
                explanation.append("Apartment living is suitable for this size. ");
                score += 20.0;
            }
        } else {
            explanation.append("A house provides good space. ");
            score += 20.0;
        }
        
        if (Boolean.TRUE.equals(quiz.getHasChildren())) {
            String petTemperament = pet.getDescription();
            if (petTemperament != null && petTemperament.toLowerCase().contains("friendly")) {
                explanation.append("Friendly pet is great with children. ");
                score += 20.0;
            } else {
                explanation.append("Unknown temperament with children. ");
                score += 10.0;
            }
        } else {
            score += 20.0;
        }
        
        return score;
    }
}
