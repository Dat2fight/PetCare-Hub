package com.petcarehub.strategy.matching;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.Period;

@Component
public class ActivityLevelStrategy implements PetMatchingStrategy {
    @Override
    public double calculateMatchScore(PetMatchingQuiz quiz, Pet pet, StringBuilder explanation) {
        String desiredActivity = quiz.getActivityLevel();
        if (desiredActivity == null) return 0.0;
        
        int age = pet.getDateOfBirth() != null ? Period.between(pet.getDateOfBirth(), LocalDate.now()).getYears() : 2;
        
        if ("HIGH".equalsIgnoreCase(desiredActivity) && age < 3) {
            explanation.append("High activity level matches well with a young, energetic pet. ");
            return 30.0;
        } else if ("LOW".equalsIgnoreCase(desiredActivity) && age > 5) {
            explanation.append("Low activity level matches well with an older, calmer pet. ");
            return 30.0;
        }
        
        explanation.append("Activity level is an acceptable match. ");
        return 15.0;
    }
}
