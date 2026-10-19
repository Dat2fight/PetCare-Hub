package com.petcarehub.config;

import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app.pets")
public class PersonalPetProperties {
    // NEEDS DECISION: limit personal registrations to 20; count all currently owned pets.
    @Positive
    private int maxPersonalPets = 20;
}
