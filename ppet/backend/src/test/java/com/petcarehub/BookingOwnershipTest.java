package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class BookingOwnershipTest {
    @Autowired MockMvc mvc;
    @MockBean PetRepository pets;
    @MockBean UserRepository users;
    @MockBean AppointmentRepository appointments;
    UserDetailsImpl caller = new UserDetailsImpl(1L,"customer","c@example.test","unused",List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    @BeforeEach void setup() {
        var account = new User(); account.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(account));
    }
    String request() { return "{\"petId\":5,\"customerId\":2,\"serviceId\":1,\"appointmentTime\":\"" + LocalDate.now().plusDays(1) + "T10:00:00\"}"; }
    @Test void foreignPetCannotBeBookedEvenWithSpoofedCustomerId() throws Exception {
        var owner = new User(); owner.setId(2L);
        when(pets.findById(5L)).thenReturn(Optional.of(Pet.builder().owner(owner).build()));
        mvc.perform(post("/api/v1/appointments").with(user(caller)).contentType("application/json").content(request())).andExpect(status().isNotFound());
        verify(users,never()).findById(2L); verifyNoInteractions(appointments);
        verify(pets,never()).findByOwnerId(any());
    }
    @Test void nonexistentPetReturns404WithoutFallback() throws Exception {
        mvc.perform(post("/api/v1/appointments").with(user(caller)).contentType("application/json").content(request())).andExpect(status().isNotFound());
        verify(pets,never()).findByOwnerId(any()); verify(pets,never()).findAll(); verifyNoInteractions(appointments);
    }
    @Test void unownedStorePetCannotBeBooked() throws Exception {
        when(pets.findById(5L)).thenReturn(Optional.of(new Pet()));
        mvc.perform(post("/api/v1/appointments").with(user(caller)).contentType("application/json").content(request())).andExpect(status().isNotFound());
        verifyNoInteractions(appointments);
    }
}
