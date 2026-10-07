package com.petcarehub.state.health;

import com.petcarehub.entity.Pet;
import com.petcarehub.exception.InvalidStateTransitionException;

public class RecoveringState implements PetHealthState {
    @Override
    public PetHealthState markHealthy(Pet pet) {
        return new HealthyState();
    }

    @Override
    public PetHealthState markDueForVaccination(Pet pet) {
        throw new InvalidStateTransitionException("Cannot transition from RECOVERING to DUE_FOR_VACCINATION directly.");
    }

    @Override
    public PetHealthState markUnderTreatment(Pet pet) {
        return new UnderTreatmentState();
    }

    @Override
    public PetHealthState markRecovering(Pet pet) {
        throw new InvalidStateTransitionException("Pet is already recovering.");
    }

    @Override
    public PetHealthState markHealthWarning(Pet pet) {
        return new HealthWarningState();
    }
}
