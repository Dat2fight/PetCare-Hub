package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.AppointmentStatus;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AppointmentAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean AppointmentRepository appointments;
    @MockBean PetRepository pets;
    UserDetailsImpl caller(long id, String role) { return new UserDetailsImpl(id,"caller","c@example.test","unused",List.of(new SimpleGrantedAuthority("ROLE_"+role))); }
    User account(long id) { User u = new User(); u.setId(id); return u; }
    Appointment appointment(long owner, long staff) {
        var a = Appointment.builder().customer(account(owner)).staff(account(staff)).appointmentDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(10,0)).status(AppointmentStatus.SCHEDULED).build();
        when(appointments.findLockedById(10L)).thenReturn(Optional.of(a));
        when(appointments.save(any())).thenAnswer(call -> call.getArgument(0));
        return a;
    }
    @Test void customerCannotListAllOrAnotherCustomersAppointments() throws Exception {
        mvc.perform(get("/api/v1/appointments").with(user(caller(1,"CUSTOMER")))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/appointments/user/2").with(user(caller(1,"CUSTOMER")))).andExpect(status().isForbidden());
        verifyNoInteractions(appointments);
    }
    @Test void petHistoryIsRestrictedToCurrentCustomersAppointments() throws Exception {
        when(pets.findById(5L)).thenReturn(Optional.of(Pet.builder().owner(account(1)).build()));
        when(appointments.findByPetIdAndCustomerId(eq(5L),eq(1L),any())).thenReturn(Page.empty());
        mvc.perform(get("/api/v1/appointments/pet/5").with(user(caller(1,"CUSTOMER")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/appointments/pet/5").with(user(caller(2,"CUSTOMER")))).andExpect(status().isForbidden());
        verify(appointments,never()).findByPetId(any(),any());
    }
    @Test void customerMayOnlyCancelOwnFutureAppointment() throws Exception {
        appointment(1,3);
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(2,"CUSTOMER")))
                .contentType("application/json").content("{\"status\":\"CANCELLED\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(1,"CUSTOMER")))
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(1,"CUSTOMER")))
                .contentType("application/json").content("{\"status\":\"CANCELLED\"}")).andExpect(status().isOk());
        verify(appointments,times(1)).save(any());
    }
    @Test void assignedIdDoesNotGrantBusinessPermissionAfterRoleRemoval() throws Exception {
        appointment(1,3);
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(3,"CUSTOMER")))
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(4,"VETERINARIAN")))
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(3,"VETERINARIAN")))
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isOk());
    }
    @Test void existing409TransitionRuleIsPreserved() throws Exception {
        appointment(1,3).setStatus(AppointmentStatus.COMPLETED);
        mvc.perform(put("/api/v1/appointments/10/status").with(user(caller(4,"ADMIN")))
                .contentType("application/json").content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isConflict());
        verify(appointments,never()).save(any());
    }
}
