package com.petcarehub.dto.pet;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"ownerId", "userId", "customerId", "price", "availability", "availabilityStatus", "healthStatus"})
public class QuickPetCreateRequest {
    @NotBlank(message="Tên thú cưng là bắt buộc")
    @Size(max=100, message="Tên thú cưng tối đa 100 ký tự")
    private String name;
    @NotNull(message="Loài là bắt buộc")
    private Long speciesId;
    @NotNull(message="Tuổi là bắt buộc")
    @Min(value=0, message="Tuổi không được âm")
    @Max(value=30, message="Tuổi tối đa 30 năm")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=WholeYears.class)
    private Integer age;

    public static class WholeYears extends com.fasterxml.jackson.databind.JsonDeserializer<Integer> {
        @Override public Integer deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext context) throws java.io.IOException {
            if (!parser.hasToken(com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_INT))
                throw com.fasterxml.jackson.databind.JsonMappingException.from(parser, "age: Tuổi phải là số nguyên");
            return parser.getIntValue();
        }
    }
}
