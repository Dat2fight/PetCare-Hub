package com.petcarehub.dto.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PetCreateRequest {
    @NotBlank
    private String name;

    @NotNull
    private Long speciesId;

    private Long breedId;

    @jakarta.validation.constraints.PositiveOrZero(message="Tuổi không được âm")
    private Integer age;
    @jakarta.validation.constraints.PastOrPresent(message="Ngày sinh không được trong tương lai")
    private java.time.LocalDate birthDate;

    @NotBlank
    private String healthStatus;

    @NotBlank
    private String availabilityStatus;

    private Long ownerId;

    private String description;
    
    private String imageUrl;
    
    @jakarta.validation.constraints.DecimalMin(value="0", message="Giá không được âm")
    private java.math.BigDecimal price;
}
