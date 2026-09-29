package com.petcarehub.state.health;

import com.petcarehub.entity.Pet;
import com.petcarehub.exception.InvalidStateTransitionException;

public class DueForVaccinationState implements PetHealthState {
    @Override
    public PetHealthState markHealthy(Pet pet) {
        return new HealthyState();
    }

    @Override
    public PetHealthState markDueForVaccination(Pet pet) {
        throw new InvalidStateTransitionException("Pet is already due for vaccination.");
    }

    @Override
    public PetHealthState markUnderTreatment(Pet pet) {
        return new UnderTreatmentState();
    }

    @Override
    public PetHealthState markRecovering(Pet pet) {
        throw new InvalidStateTransitionException("Cannot transition from DUE_FOR_VACCINATION to RECOVERING.");
    }

    @Override
    public PetHealthState markHealthWarning(Pet pet) {
        return new HealthWarningState();
    }
}
