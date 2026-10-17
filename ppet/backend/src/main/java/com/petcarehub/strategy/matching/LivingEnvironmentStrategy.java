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
                explanation.append("Thú cưng lớn có thể không phù hợp căn hộ. ");
                score += 5.0;
            } else {
                explanation.append("Căn hộ phù hợp với kích thước thú cưng. ");
                score += 20.0;
            }
        } else {
            explanation.append("Nhà riêng có không gian phù hợp. ");
            score += 20.0;
        }
        
        if (Boolean.TRUE.equals(quiz.getHasChildren())) {
            String petTemperament = pet.getDescription();
            if (petTemperament != null && petTemperament.toLowerCase().contains("friendly")) {
                explanation.append("Thú cưng thân thiện phù hợp gia đình có trẻ nhỏ. ");
                score += 20.0;
            } else {
                explanation.append("Chưa rõ tính cách khi ở cùng trẻ nhỏ. ");
                score += 10.0;
            }
        } else {
            score += 20.0;
        }
        
        return score;
    }
}
