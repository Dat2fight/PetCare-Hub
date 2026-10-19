package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.appointment.AppointmentCreateRequest;
import com.petcarehub.dto.appointment.AppointmentUpdateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:capacity;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppointmentCapacityTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PetRepository pets;
    @Autowired SpeciesRepository species;
    @Autowired ServiceEntityRepository services;
    @Autowired AppointmentRepository appointments;
    @Autowired AppointmentService booking;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    User customer;
    User vet;
    AppointmentCreateRequest request;

    @BeforeEach void setup() {
        appointments.deleteAll();
        users.findAll().forEach(u -> { u.setEnabled(false); users.save(u); });
        customer = account();
        Role role = roles.findByName(RoleType.VETERINARIAN)
                .orElseGet(() -> roles.save(Role.builder().name(RoleType.VETERINARIAN).build()));
        vet = account(); vet.setRoles(Set.of(role)); users.save(vet);
        User second = account(); second.setRoles(Set.of(role)); users.save(second);
        var animal = species.save(Species.builder().name(UUID.randomUUID().toString()).build());
        var pet = pets.save(Pet.builder().name("Capacity").owner(customer).species(animal)
                .price(BigDecimal.ZERO).healthStatus(HealthStatus.HEALTHY).availabilityStatus(AvailabilityStatus.UNAVAILABLE).build());
        var service = services.save(ServiceEntity.builder().name("Capacity").serviceType(ServiceType.VETERINARY)
                .active(true).durationMinutes(60).price(BigDecimal.TEN).build());
        request = new AppointmentCreateRequest(); request.setPetId(pet.getId()); request.setServiceId(service.getId());
        request.setAppointmentTime(LocalDate.now().plusDays(2).atTime(10, 0));
    }

    private User account() {
        String name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused")
                .firstName("Test").lastName("Capacity").enabled(true).build());
    }

    private void authenticate() {
        var principal = UserDetailsImpl.build(customer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @Test void concurrentAutoAssignmentCannotExceedTwoAvailableStaff() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        CountDownLatch ready = new CountDownLatch(3), start = new CountDownLatch(1);
        Callable<Boolean> book = () -> {
            ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS)); authenticate();
            try { booking.bookAppointment(request); return true; }
            catch (IllegalStateException full) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            List<Future<Boolean>> calls = List.of(pool.submit(book), pool.submit(book), pool.submit(book));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            int success = 0;
            for (Future<Boolean> call : calls) if (call.get(20, TimeUnit.SECONDS)) success++;
            assertEquals(2, success);
            var saved = appointments.findAll(); assertEquals(2, saved.size());
            assertEquals(2, saved.stream().map(a -> a.getStaff().getId()).distinct().count());
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void allStaffBusyReturns409WithoutFallbackOrExtraRow() throws Exception {
        authenticate();
        try { booking.bookAppointment(request); booking.bookAppointment(request); }
        finally { SecurityContextHolder.clearContext(); }
        mvc.perform(post("/api/v1/appointments").with(user(UserDetailsImpl.build(customer)))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isConflict());
        assertEquals(2, appointments.count());
    }

    @Test void explicitStaffConflictRejectsButAdjacentSlotAndCancellationRemainValid() {
        authenticate(); request.setStaffId(vet.getId());
        try {
            var first = booking.bookAppointment(request);
            assertThrows(IllegalStateException.class, () -> booking.bookAppointment(request));
            request.setAppointmentTime(request.getAppointmentTime().plusHours(1));
            booking.bookAppointment(request); // Half-open intervals: 10-11 and 11-12 do not overlap.
            var cancel = new AppointmentUpdateRequest(); cancel.setStatus(AppointmentStatus.CANCELLED);
            booking.updateAppointmentStatus(first.getId(), cancel);
            request.setAppointmentTime(request.getAppointmentTime().minusHours(1));
            booking.bookAppointment(request);
            assertEquals(3, appointments.count());
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void noEligibleStaffReturns409WithoutSaving() throws Exception {
        users.findEligibleStaff(RoleType.VETERINARIAN).forEach(u -> { u.setEnabled(false); users.save(u); });
        mvc.perform(post("/api/v1/appointments").with(user(UserDetailsImpl.build(customer)))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isConflict());
        assertEquals(0, appointments.count());
    }
}
