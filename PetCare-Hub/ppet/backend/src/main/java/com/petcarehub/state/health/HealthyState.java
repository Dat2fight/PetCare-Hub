package com.petcarehub.state.health;

import com.petcarehub.entity.Pet;
import com.petcarehub.exception.InvalidStateTransitionException;

public class HealthyState implements PetHealthState {
    @Override
    public PetHealthState markHealthy(Pet pet) {
        throw new InvalidStateTransitionException("Pet is already healthy.");
    }

    @Override
    public PetHealthState markDueForVaccination(Pet pet) {
        return new DueForVaccinationState();
    }

    @Override
    public PetHealthState markUnderTreatment(Pet pet) {
        return new UnderTreatmentState();
    }

    @Override
    public PetHealthState markRecovering(Pet pet) {
        throw new InvalidStateTransitionException("Cannot transition directly from HEALTHY to RECOVERING.");
    }

    @Override
    public PetHealthState markHealthWarning(Pet pet) {
        return new HealthWarningState();
    }
}
