package com.petcarehub.state.health;

import com.petcarehub.entity.Pet;

public interface PetHealthState {
    PetHealthState markHealthy(Pet pet);
    PetHealthState markDueForVaccination(Pet pet);
    PetHealthState markUnderTreatment(Pet pet);
    PetHealthState markRecovering(Pet pet);
    PetHealthState markHealthWarning(Pet pet);
}
