package com.petcarehub.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BcryptPasswordLengthValidator.class)
public @interface BcryptPasswordLength {
    String message() default "Mật khẩu tối đa 72 byte UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
