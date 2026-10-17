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
            explanation.append("Mức vận động cao phù hợp với thú cưng trẻ, năng động. ");
            return 30.0;
        } else if ("LOW".equalsIgnoreCase(desiredActivity) && age > 5) {
            explanation.append("Mức vận động thấp phù hợp với thú cưng lớn tuổi, điềm tĩnh. ");
            return 30.0;
        }
        
        explanation.append("Mức vận động tương đối phù hợp. ");
        return 15.0;
    }
}
