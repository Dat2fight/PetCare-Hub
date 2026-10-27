package com.petcarehub.dto.pet;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class PersonalPetCreateRequest {
    @NotBlank(message="Tên thú cưng là bắt buộc") @Size(max=100, message="Tên tối đa 100 ký tự")
    private String name;
    @NotNull(message="Vui lòng chọn loài") private Long speciesId;
    private Long breedId;
    @Min(value=0, message="Tuổi không được âm") @Max(value=30, message="Tuổi tối đa 30 năm")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=WholeYears.class)
    private Integer age;
    // Keep the existing MyPets form compatible; birth date may be unknown.
    @PastOrPresent(message="Ngày sinh không được trong tương lai") private java.time.LocalDate birthDate;
    private String description;
    private String imageUrl;
    public static class WholeYears extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext c) throws java.io.IOException {
            if (!p.hasToken(com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT))
                throw com.fasterxml.jackson.databind.JsonMappingException.from(p, "Tuổi phải là số nguyên");
            return p.getIntValue();
        }
    }
}
