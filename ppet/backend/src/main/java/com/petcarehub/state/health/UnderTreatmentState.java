package com.petcarehub.state.health;

import com.petcarehub.entity.Pet;
import com.petcarehub.exception.InvalidStateTransitionException;

public class UnderTreatmentState implements PetHealthState {
    @Override
    public PetHealthState markHealthy(Pet pet) {
        throw new InvalidStateTransitionException("Cannot transition directly from UNDER_TREATMENT to HEALTHY. Must recover first.");
    }

    @Override
    public PetHealthState markDueForVaccination(Pet pet) {
        throw new InvalidStateTransitionException("Cannot transition from UNDER_TREATMENT to DUE_FOR_VACCINATION.");
    }

    @Override
    public PetHealthState markUnderTreatment(Pet pet) {
        throw new InvalidStateTransitionException("Pet is already under treatment.");
    }

    @Override
    public PetHealthState markRecovering(Pet pet) {
        return new RecoveringState();
    }

    @Override
    public PetHealthState markHealthWarning(Pet pet) {
        return new HealthWarningState();
    }
}
