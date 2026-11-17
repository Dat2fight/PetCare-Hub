package com.petcarehub.strategy.matching;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.List;

@Component @Order(2)
public class ActivityLevelStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        var levels = List.of("LOW", "MEDIUM", "HIGH");
        if (quiz.getActivityLevel() == null || pet.getActivityLevel() == null) return 0;
        int desired = levels.indexOf(quiz.getActivityLevel()), actual = levels.indexOf(pet.getActivityLevel());
        if (desired < 0 || actual < 0) return 0;
        int points = 30 - 15 * Math.abs(desired - actual);
        explanation.append("Vận động: hồ sơ bé ghi mức ").append(MatchingProfile.activityLabel(pet.getActivityLevel()))
                .append(", bạn chọn mức ").append(MatchingProfile.activityLabel(quiz.getActivityLevel()))
                .append(" (").append(points).append("/30 điểm).\n");
        return points;
    }
}
