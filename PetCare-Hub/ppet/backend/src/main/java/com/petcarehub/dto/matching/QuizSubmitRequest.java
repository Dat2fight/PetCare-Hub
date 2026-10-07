package com.petcarehub.dto.matching;

import lombok.Data;

@Data
public class QuizSubmitRequest {
    private String houseSize;
    private Boolean presenceOfChildren;
    private String activityLevel;
    private Integer availableCareTime;
}
