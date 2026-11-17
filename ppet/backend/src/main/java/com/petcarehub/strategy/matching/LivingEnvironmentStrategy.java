package com.petcarehub.strategy.matching;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component @Order(1)
public class LivingEnvironmentStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        int score = 0;
        if ("HOUSE".equals(quiz.getLivingEnvironment())) {
            explanation.append("Không gian: bạn chọn nhà riêng, không yêu cầu bé phù hợp căn hộ (20/20 điểm).\n");
            score += 20;
        } else if ("APARTMENT".equals(quiz.getLivingEnvironment()) && Boolean.TRUE.equals(pet.getSuitableForApartment())) {
            explanation.append("Không gian: hồ sơ bé ghi phù hợp sống trong căn hộ (20/20 điểm).\n");
            score += 20;
        }
        else if ("APARTMENT".equals(quiz.getLivingEnvironment()) && Boolean.FALSE.equals(pet.getSuitableForApartment())) {
            explanation.append("Không gian: hồ sơ bé ghi không phù hợp căn hộ bạn chọn (0/20 điểm).\n");
        }
        if (Boolean.FALSE.equals(quiz.getHasChildren())) {
            explanation.append("Gia đình: bạn không có trẻ nhỏ, không yêu cầu tiêu chí này (20/20 điểm).\n");
            score += 20;
        } else if (Boolean.TRUE.equals(quiz.getHasChildren()) && Boolean.TRUE.equals(pet.getGoodWithChildren())) {
            explanation.append("Gia đình: hồ sơ bé ghi phù hợp với trẻ nhỏ (20/20 điểm).\n");
            score += 20;
        }
        else if (Boolean.TRUE.equals(quiz.getHasChildren()) && Boolean.FALSE.equals(pet.getGoodWithChildren())) {
            explanation.append("Gia đình: hồ sơ bé ghi không phù hợp với trẻ nhỏ trong gia đình bạn (0/20 điểm).\n");
        }
        return score;
    }
}
