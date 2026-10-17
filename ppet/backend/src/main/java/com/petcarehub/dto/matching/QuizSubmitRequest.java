package com.petcarehub.dto.matching;

import lombok.Data;

@Data
public class QuizSubmitRequest {
    @jakarta.validation.constraints.NotBlank(message="Môi trường sống là bắt buộc")
    private String livingEnvironment;
    private String houseSize;
    @jakarta.validation.constraints.NotNull(message="Vui lòng trả lời về trẻ nhỏ")
    private Boolean presenceOfChildren;
    @jakarta.validation.constraints.NotBlank(message="Mức hoạt động là bắt buộc")
    private String activityLevel;
    @jakarta.validation.constraints.NotNull(message="Thời gian chăm sóc là bắt buộc")
    @jakarta.validation.constraints.Min(value=0, message="Số giờ không được âm")
    @jakarta.validation.constraints.Max(value=24, message="Số giờ tối đa là 24")
    private Integer availableCareTime;
}
