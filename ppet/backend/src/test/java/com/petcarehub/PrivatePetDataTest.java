package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class PrivatePetDataTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired SpeciesRepository species;
    @Autowired PetRepository pets;
    @Autowired MedicalRecordRepository medical;
    @Autowired VaccinationRepository vaccinations;
    @Autowired NotificationRepository notifications;
    User owner, other;
    Pet mine, sale, unavailable;
    Species animal;
    MedicalRecord record;
    Vaccination vaccine;
    Notification notice;

    User account() {
        String name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused-test-hash")
                .firstName("Test").lastName("User").build());
    }
    UserDetailsImpl caller(User u, String role) {
        return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));
    }
    @BeforeEach void setup() {
        owner = account(); other = account();
        animal = species.save(Species.builder().name(UUID.randomUUID().toString()).build());
        // Even an inconsistent owned + AVAILABLE row must never be public.
        mine = pets.save(Pet.builder().name("Private").species(animal).owner(owner).price(BigDecimal.ONE).build());
        sale = pets.save(Pet.builder().name("Sale").species(animal).price(BigDecimal.TEN).build());
        unavailable = pets.save(Pet.builder().name("Unavailable").species(animal).price(BigDecimal.ONE).availabilityStatus(AvailabilityStatus.UNAVAILABLE).build());
        record = medical.save(MedicalRecord.builder().pet(mine).veterinarian(other).examinationDate(LocalDate.now()).findings("Private findings").build());
        vaccine = vaccinations.save(Vaccination.builder().pet(mine).veterinarian(other).vaccineName("Test vaccine").dateAdministered(LocalDate.now()).build());
        notice = notifications.save(Notification.builder().user(owner).title("Private").message("Private message").type(NotificationType.values()[0]).build());
    }
    @Test void publicPagesOnlyReturnUnownedAvailablePetsWithoutPrivateFields() throws Exception {
        mvc.perform(get("/api/v1/pets").param("speciesId",animal.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sale.getId()))
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist()).andExpect(jsonPath("$.content[0].healthStatus").doesNotExist());
        mvc.perform(get("/api/v1/pets/"+sale.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").doesNotExist()).andExpect(jsonPath("$.healthStatus").doesNotExist());
        for (Pet p : List.of(mine,unavailable)) mvc.perform(get("/api/v1/pets/"+p.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pets").param("speciesId",animal.getId().toString()).param("availabilityStatus","UNAVAILABLE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void privatePetEndpointRequiresOwnerOrClinicalRole() throws Exception {
        String path = "/api/v1/pets/my-pets/"+mine.getId();
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(get(path).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.getId()));
        for (String role : List.of("VETERINARIAN","ADMIN")) mvc.perform(get(path).with(user(caller(other,role)))).andExpect(status().isOk());
    }
    @Test void clinicalDetailsAndHistoriesRejectOtherCustomersAndPreserveClinicalAccess() throws Exception {
        for (String path : List.of("/api/v1/medical-records/"+record.getId(), "/api/v1/medical-records/pet/"+mine.getId(),
                "/api/v1/vaccinations/"+vaccine.getId(), "/api/v1/vaccinations/pet/"+mine.getId())) {
            mvc.perform(get(path).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isOk());
            for (String role : List.of("VETERINARIAN","ADMIN")) mvc.perform(get(path).with(user(caller(other,role)))).andExpect(status().isOk());
        }
        mvc.perform(delete("/api/v1/medical-records/"+record.getId()).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/vaccinations/"+vaccine.getId()).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isForbidden());
    }
    @Test void notificationsStayStrictlyPerPrincipalIncludingAdmin() throws Exception {
        for (String role : List.of("CUSTOMER","ADMIN")) {
            mvc.perform(get("/api/v1/notifications/"+owner.getId()).with(user(caller(other,role)))).andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/notifications/"+notice.getId()+"/read").with(user(caller(other,role)))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/api/v1/notifications").with(user(caller(owner,"CUSTOMER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(notice.getId()));
        mvc.perform(put("/api/v1/notifications/read-all").with(user(caller(other,"CUSTOMER")))).andExpect(status().isOk());
        assertFalse(notifications.findById(notice.getId()).orElseThrow().getRead());
    }
}
