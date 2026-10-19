package com.petcarehub.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class BcryptPasswordLengthValidator implements ConstraintValidator<BcryptPasswordLength, String> {
    @Override public boolean isValid(String value, ConstraintValidatorContext context) {
        // BCrypt's limit is bytes, not Java characters; NotBlank handles missing values.
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
