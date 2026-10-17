package com.petcarehub.strategy.matching;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class CareTimeStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        BigDecimal careTime = quiz.getDailyCareHours();
        if (careTime == null) return 0.0;
        
        if (careTime.compareTo(new BigDecimal("2")) < 0) {
            explanation.append("Thời gian chăm sóc ít có thể gây khó khăn. ");
            return 10.0;
        } else if (careTime.compareTo(new BigDecimal("2")) >= 0 && careTime.compareTo(new BigDecimal("4")) < 0) {
            explanation.append("Thời gian chăm sóc vừa phải đáp ứng nhu cầu cơ bản. ");
            return 20.0;
        } else {
            explanation.append("Bạn có nhiều thời gian quan tâm thú cưng. ");
            return 30.0;
        }
    }
}
