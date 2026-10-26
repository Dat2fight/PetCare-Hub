package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class QaAppointmentScopeTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired RoleRepository roles;
    @Autowired SpeciesRepository species; @Autowired PetRepository pets; @Autowired ServiceEntityRepository services;
    @Autowired AppointmentRepository appointments; @Autowired JwtTokenProvider tokens;
    User a,b,staff,otherStaff,admin; Pet petA,petB; Appointment apA,apB;
    User account(RoleType role) {
        var r=roles.findByName(role).orElseGet(()->roles.save(Role.builder().name(role).build()));
        String n="qa_"+UUID.randomUUID();
        return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused")
                .firstName("qa_First").lastName("qa_Last").roles(Set.of(r)).build());
    }
    String token(User u) {
        var p=UserDetailsImpl.build(u);
        return "Bearer "+tokens.generateToken(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));
    }
    @BeforeEach void seed() {
        a=account(RoleType.CUSTOMER);b=account(RoleType.CUSTOMER);staff=account(RoleType.VETERINARIAN);
        otherStaff=account(RoleType.VETERINARIAN);admin=account(RoleType.ADMIN);
        var s=species.save(Species.builder().name("qa_"+UUID.randomUUID()).build());
        petA=pets.save(Pet.builder().name("qa_A").species(s).owner(a).price(BigDecimal.ZERO).build());
        petB=pets.save(Pet.builder().name("qa_B").species(s).owner(b).price(BigDecimal.ZERO).build());
        var svc=services.save(ServiceEntity.builder().name("qa_visit").serviceType(ServiceType.VETERINARY).durationMinutes(30).price(BigDecimal.TEN).build());
        apA=appointments.save(Appointment.builder().customer(a).pet(petA).staff(staff).service(svc).appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10,0)).endTime(LocalTime.of(10,30)).notes("qa_private_A").build());
        apB=appointments.save(Appointment.builder().customer(b).pet(petB).staff(otherStaff).service(svc).appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(11,0)).endTime(LocalTime.of(11,30)).notes("qa_private_B").build());
    }
    @Test void staffListsOnlyAssignedAppointments() throws Exception {
        mvc.perform(get("/api/v1/appointments").header("Authorization",token(staff)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(apA.getId()));
        mvc.perform(get("/api/v1/appointments").header("Authorization",token(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }
    @Test void customerCannotDiscoverOrCancelAnotherCustomersAppointment() throws Exception {
        mvc.perform(get("/api/v1/appointments/user/{id}",b.getId()).header("Authorization",token(a))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/appointments/pet/{id}",petB.getId()).header("Authorization",token(a)))
                .andExpect(status().isNotFound()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("qa_private_B"))));
        mvc.perform(put("/api/v1/appointments/{id}/status",apB.getId()).header("Authorization",token(a))
                .contentType("application/json").content("{\"status\":\"CANCELLED\"}")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/appointments").header("Authorization",token(a))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/appointments/user/{id}",a.getId()).header("Authorization",token(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(apA.getId()));
        mvc.perform(get("/api/v1/appointments/me").header("Authorization",token(a)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(apA.getId()));
    }
}
