package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.health.TreatmentCreateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.TreatmentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:treatment-flow;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class TreatmentFlowTest {
    @Autowired UserRepository users; @Autowired PetRepository pets; @Autowired SpeciesRepository species;
    @Autowired MedicalRecordRepository records; @Autowired TreatmentRepository treatments;
    @Autowired TreatmentService service; @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    User owner,other; MedicalRecord record,foreignRecord; TreatmentCreateRequest request;
    User account() { String n=UUID.randomUUID().toString();return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build()); }
    UserDetailsImpl caller(User u,String role) {return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
    MedicalRecord record(User owner) {
        var kind=species.save(Species.builder().name(UUID.randomUUID().toString()).build());
        var pet=pets.save(Pet.builder().name("Pet").species(kind).owner(owner).price(BigDecimal.ZERO).build());
        return records.save(MedicalRecord.builder().pet(pet).veterinarian(other).examinationDate(LocalDate.now()).findings("Exam").build());
    }
    @BeforeEach void setup() {
        owner=account();other=account();record=record(owner);foreignRecord=record(other);
        request=new TreatmentCreateRequest();request.setMedicalRecordId(record.getId());request.setMedicine("Medicine");
        request.setDosage("1 dose");request.setFrequency("Daily");request.setStartDate(LocalDate.now());request.setDescription("Notes");
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    @Test void veterinarianAndAdminCrudPersistsAllRequiredFields() throws Exception {
        var response=mvc.perform(post("/api/v1/treatments").with(user(caller(other,"VETERINARIAN")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isCreated()).andReturn();
        long id=json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        var stored=treatments.findById(id).orElseThrow();assertEquals("Medicine",stored.getMedication());assertEquals("Daily",stored.getFrequency());
        assertEquals(request.getStartDate(),stored.getStartDate());assertEquals("Notes",stored.getNotes());
        request.setMedicine("Updated");
        mvc.perform(put("/api/v1/treatments/{id}",id).with(user(caller(other,"ADMIN")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.medicine").value("Updated"));
        mvc.perform(get("/api/v1/treatments/{id}",id).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/treatments/{id}",id).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/treatments/record/{id}",record.getId()).with(user(caller(other,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/treatments/{id}",id).with(user(caller(owner,"CUSTOMER")))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/treatments/{id}",id).with(user(caller(other,"ADMIN")))).andExpect(status().isNoContent());
        assertFalse(treatments.existsById(id));
    }
    @Test void reassignmentCannotBypassExistingOwnershipAtServiceBoundary() {
        var principal=caller(other,"VETERINARIAN");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        var treatment=service.createTreatment(request);
        principal=caller(other,"CUSTOMER");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        request.setMedicalRecordId(foreignRecord.getId());
        assertThrows(com.petcarehub.exception.ResourceNotFoundException.class,()->service.updateTreatment(treatment.getId(),request));
    }
    @Test void invalidDatesAndMissingRecordAreRejectedAndCustomerCannotCreate() throws Exception {
        mvc.perform(post("/api/v1/treatments").with(user(caller(owner,"CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isForbidden());
        request.setEndDate(request.getStartDate().minusDays(1));
        mvc.perform(post("/api/v1/treatments").with(user(caller(other,"VETERINARIAN")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
        request.setEndDate(null);request.setMedicalRecordId(Long.MAX_VALUE);
        mvc.perform(post("/api/v1/treatments").with(user(caller(other,"VETERINARIAN")))
                .contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isNotFound());
    }
}
