package com.petcarehub.service.impl;

import com.petcarehub.entity.Pet;
import com.petcarehub.enums.HealthStatus;
import com.petcarehub.exception.InvalidStateTransitionException;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.service.PetHealthService;
import com.petcarehub.state.health.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PetHealthServiceImpl implements PetHealthService {

    private final PetRepository petRepository;

    public PetHealthServiceImpl(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    @Transactional
    public void updatePetHealthStatus(Long petId, HealthStatus newStatus) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Pet not found with id " + petId));

        PetHealthState currentState = mapStatusToState(pet.getHealthStatus());

        switch (newStatus) {
            case HEALTHY:
                currentState.markHealthy(pet);
                break;
            case DUE_FOR_VACCINATION:
                currentState.markDueForVaccination(pet);
                break;
            case UNDER_TREATMENT:
                currentState.markUnderTreatment(pet);
                break;
            case RECOVERING:
                currentState.markRecovering(pet);
                break;
            case HEALTH_WARNING:
                currentState.markHealthWarning(pet);
                break;
            default:
                throw new IllegalArgumentException("Unknown status: " + newStatus);
        }

        pet.setHealthStatus(newStatus);
        petRepository.save(pet);
    }

    private PetHealthState mapStatusToState(HealthStatus status) {
        if (status == null) {
            return new HealthyState();
        }
        switch (status) {
            case HEALTHY:
                return new HealthyState();
            case DUE_FOR_VACCINATION:
                return new DueForVaccinationState();
            case UNDER_TREATMENT:
                return new UnderTreatmentState();
            case RECOVERING:
                return new RecoveringState();
            case HEALTH_WARNING:
                return new HealthWarningState();
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }
}
