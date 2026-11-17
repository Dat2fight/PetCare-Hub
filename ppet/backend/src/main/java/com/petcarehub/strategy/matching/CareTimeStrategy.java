package com.petcarehub.strategy.matching;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(3)
public class CareTimeStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        if (quiz.getDailyCareHours() == null || pet.getMinimumDailyCareHours() == null || pet.getMinimumDailyCareHours().signum() <= 0) return 0;
        int points = quiz.getDailyCareHours().compareTo(pet.getMinimumDailyCareHours()) >= 0 ? 30
                : quiz.getDailyCareHours().multiply(java.math.BigDecimal.valueOf(30))
                    .divide(pet.getMinimumDailyCareHours(), 0, java.math.RoundingMode.DOWN).intValue();
        explanation.append("Chăm sóc: bé cần tối thiểu ")
                .append(pet.getMinimumDailyCareHours().stripTrailingZeros().toPlainString())
                .append(" giờ/ngày; bạn có ").append(quiz.getDailyCareHours().stripTrailingZeros().toPlainString())
                .append(" giờ/ngày (").append(points).append("/30 điểm).\n");
        return points;
    }
}
