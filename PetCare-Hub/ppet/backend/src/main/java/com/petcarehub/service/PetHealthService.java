package com.petcarehub.service;

import com.petcarehub.enums.HealthStatus;

public interface PetHealthService {
    void updatePetHealthStatus(Long petId, HealthStatus newStatus);
}
