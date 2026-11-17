package com.petcarehub.strategy.matching;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.PetMatchingQuiz;
import com.petcarehub.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.Set;

/** Explicit, per-animal observations entered by catalog staff. Null is unknown. */
public final class MatchingProfile {
    private MatchingProfile() {}
    private static final Set<String> LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    public static void apply(Pet pet, String activity, Boolean apartment, Boolean children, BigDecimal hours) {
        if (activity != null && !LEVELS.contains(activity))
            throw new BadRequestException("Mức vận động phải là LOW, MEDIUM hoặc HIGH");
        if (hours != null && (hours.signum() <= 0 || hours.compareTo(BigDecimal.valueOf(24)) > 0
                || hours.stripTrailingZeros().scale() > 2))
            throw new BadRequestException("Số giờ chăm sóc tối thiểu phải lớn hơn 0, không quá 24 và tối đa 2 chữ số thập phân");
        pet.setActivityLevel(activity);
        pet.setSuitableForApartment(apartment);
        pet.setGoodWithChildren(children);
        pet.setMinimumDailyCareHours(hours);
    }

    public static boolean complete(Pet pet) {
        return pet.getActivityLevel() != null && LEVELS.contains(pet.getActivityLevel())
                && pet.getSuitableForApartment() != null && pet.getGoodWithChildren() != null
                && pet.getMinimumDailyCareHours() != null && pet.getMinimumDailyCareHours().signum() > 0
                && pet.getMinimumDailyCareHours().compareTo(BigDecimal.valueOf(24)) <= 0;
    }

    public static boolean eligible(PetMatchingQuiz quiz, Pet pet) {
        return complete(pet) && pet.getReservedOrder() == null
                && unmetCriteria(quiz, pet).isEmpty();
    }

    /** Called only for complete profiles, so unknown values never become mismatches or guesses. */
    public static java.util.List<String> unmetCriteria(PetMatchingQuiz quiz, Pet pet) {
        var unmet = new java.util.ArrayList<String>();
        if ("APARTMENT".equals(quiz.getLivingEnvironment()) && !pet.getSuitableForApartment())
            unmet.add("Bạn ở căn hộ nhưng hồ sơ bé ghi không phù hợp sống trong căn hộ.");
        if (Boolean.TRUE.equals(quiz.getHasChildren()) && !pet.getGoodWithChildren())
            unmet.add("Gia đình bạn có trẻ nhỏ nhưng hồ sơ bé ghi không phù hợp với trẻ nhỏ.");
        var levels = java.util.List.of("LOW", "MEDIUM", "HIGH");
        if (levels.indexOf(quiz.getActivityLevel()) < levels.indexOf(pet.getActivityLevel()))
            unmet.add("Bé cần mức vận động " + activityLabel(pet.getActivityLevel())
                    + ", cao hơn mức " + activityLabel(quiz.getActivityLevel()) + " bạn chọn.");
        if (quiz.getDailyCareHours().compareTo(pet.getMinimumDailyCareHours()) < 0)
            unmet.add("Bé cần tối thiểu " + pet.getMinimumDailyCareHours().stripTrailingZeros().toPlainString()
                    + " giờ chăm sóc/ngày; bạn có " + quiz.getDailyCareHours().stripTrailingZeros().toPlainString() + " giờ/ngày.");
        return java.util.List.copyOf(unmet);
    }

    public static String activityLabel(String value) {
        return switch (value) { case "LOW" -> "thấp"; case "MEDIUM" -> "vừa"; case "HIGH" -> "cao"; default -> "chưa đánh giá"; };
    }
}
