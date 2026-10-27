package com.petcarehub.service.impl;

import com.petcarehub.dto.pet.PetUpdateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.mapper.PetMapper;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PetAuditTest {
    @Test void updatesPreservePriceAndUseHealthStateMachine() {
        var pets = mock(PetRepository.class); var species = mock(SpeciesRepository.class);
        var breeds = mock(BreedRepository.class); var users = mock(UserRepository.class);
        var health = new PetHealthServiceImpl(pets);
        var service = new PetServiceImpl(health, pets, species, breeds, users, mock(com.petcarehub.security.Ownership.class), new com.petcarehub.config.PersonalPetProperties());
        Species dog = new Species(); dog.setId(1L);
        Pet pet = Pet.builder().species(dog).price(BigDecimal.TEN).healthStatus(HealthStatus.UNDER_TREATMENT).availabilityStatus(AvailabilityStatus.AVAILABLE).build(); pet.setId(1L);
        when(pets.findById(1L)).thenReturn(Optional.of(pet)); when(pets.findByIdForUpdate(1L)).thenReturn(Optional.of(pet)); when(species.findById(1L)).thenReturn(Optional.of(dog)); when(pets.save(pet)).thenReturn(pet);
        PetUpdateRequest request = new PetUpdateRequest(); request.setName("Pet"); request.setSpeciesId(1L); request.setHealthStatus("HEALTHY"); request.setAvailabilityStatus("AVAILABLE");
        assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class, () -> service.updatePet(1L, request));
        request.setHealthStatus("RECOVERING"); service.updatePet(1L, request); assertEquals(HealthStatus.RECOVERING, pet.getHealthStatus());
        request.setHealthStatus("HEALTHY"); service.updatePet(1L, request); assertEquals(BigDecimal.TEN, pet.getPrice());
        request.setHealthStatus("INVALID"); assertThrows(com.petcarehub.exception.BadRequestException.class, () -> service.updatePet(1L, request));
        request.setHealthStatus("HEALTHY"); request.setOwnerId(99L); assertThrows(com.petcarehub.exception.ResourceNotFoundException.class, () -> service.updatePet(1L, request));
        request.setOwnerId(null); request.setBreedId(2L);
        Species cat = new Species(); cat.setId(2L); Breed breed = new Breed(); breed.setSpecies(cat);
        when(breeds.findById(2L)).thenReturn(Optional.of(breed));
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> service.updatePet(1L, request));
    }

    @Test void mapperCalculatesAgeAndHidesPrivatePetPrice() {
        Pet pet = Pet.builder().dateOfBirth(LocalDate.now().minusYears(3)).price(BigDecimal.TEN).availabilityStatus(AvailabilityStatus.AVAILABLE).build();
        assertEquals(3, PetMapper.toDTO(pet).getAge()); assertEquals(BigDecimal.TEN, PetMapper.toDTO(pet).getPrice());
        pet.setOwner(new User()); assertNull(PetMapper.toDTO(pet).getPrice());
    }

    @Test void matchingStrategiesUseKnownAnswers() {
        Pet pet = Pet.builder().dateOfBirth(LocalDate.now().minusYears(1)).weight(BigDecimal.valueOf(5)).description("friendly").build();
        PetMatchingQuiz quiz = PetMatchingQuiz.builder().activityLevel("HIGH").dailyCareHours(BigDecimal.valueOf(4)).livingEnvironment("APARTMENT").hasChildren(true).build();
        assertEquals(30, new com.petcarehub.strategy.matching.ActivityLevelStrategy().calculateMatchScore(quiz, pet, new StringBuilder()));
        assertEquals(30, new com.petcarehub.strategy.matching.CareTimeStrategy().calculateMatchScore(quiz, pet, new StringBuilder()));
        assertEquals(40, new com.petcarehub.strategy.matching.LivingEnvironmentStrategy().calculateMatchScore(quiz, pet, new StringBuilder()));
    }

    @Test void quizAnswersAreMappedExplicitly() {
        var pets=mock(PetRepository.class); var users=mock(UserRepository.class);
        var quizzes=mock(PetMatchingQuizRepository.class); var results=mock(PetMatchingResultRepository.class);
        var service=new PetMatchingServiceImpl(java.util.List.of(), pets, users, quizzes, results);
        User customer=new User(); customer.setId(7L);
        when(users.findById(7L)).thenReturn(Optional.of(customer));
        when(quizzes.save(any())).thenAnswer(call->call.getArgument(0));
        when(pets.findByOwnerIsNullAndAvailabilityStatus(eq(AvailabilityStatus.AVAILABLE),any())).thenReturn(org.springframework.data.domain.Page.empty());
        var request=new com.petcarehub.dto.matching.QuizSubmitRequest();
        request.setPresenceOfChildren(true); request.setAvailableCareTime(4); request.setActivityLevel("HIGH"); request.setHouseSize("SMALL"); request.setLivingEnvironment("APARTMENT");
        assertTrue(service.matchPets(7L,request).isEmpty());
        var captured=org.mockito.ArgumentCaptor.forClass(PetMatchingQuiz.class); verify(quizzes).save(captured.capture());
        var quiz=captured.getValue(); assertTrue(quiz.getHasChildren()); assertEquals(BigDecimal.valueOf(4),quiz.getDailyCareHours());
        assertEquals("HIGH",quiz.getActivityLevel()); assertEquals("SMALL",quiz.getHouseSize()); assertEquals("APARTMENT",quiz.getLivingEnvironment()); assertEquals(7L,quiz.getCustomer().getId());
    }
}
