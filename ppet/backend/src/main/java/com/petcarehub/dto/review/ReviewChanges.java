package com.petcarehub.dto.review;
import jakarta.validation.constraints.*;
public record ReviewChanges(@NotNull @Min(1) @Max(5) Integer rating, @NotBlank @Size(max=2000) String comment) {}
