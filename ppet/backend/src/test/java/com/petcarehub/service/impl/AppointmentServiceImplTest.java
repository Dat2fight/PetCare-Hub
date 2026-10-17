package com.petcarehub.service.impl;

import com.petcarehub.config.BookingProperties;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentServiceImplTest {
    final AppointmentRepository appointments = mock(AppointmentRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final EntityManager em = mock(EntityManager.class);
    final AppointmentServiceImpl service = new AppointmentServiceImpl(appointments, mock(PetRepository.class), users, em, new BookingProperties());

    @Test void invalidDurationsTerminateAndAvailabilityUsesRealConflicts() {
        ServiceEntity treatment = ServiceEntity.builder().active(true).serviceType(ServiceType.VETERINARY).build();
        when(em.find(ServiceEntity.class, 1L)).thenReturn(treatment);
        for (int duration : new int[]{0, -1, 1440, 481}) {
            treatment.setDurationMinutes(duration);
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> assertTrue(service.getAvailableTimeSlots(LocalDate.now().plusDays(1), 1L).isEmpty()));
        }
        treatment.setDurationMinutes(60);
        User vet = User.builder().enabled(true).roles(Set.of(Role.builder().name(RoleType.VETERINARIAN).build())).build(); vet.setId(2L);
        when(users.findEligibleStaff(RoleType.VETERINARIAN)).thenReturn(List.of(vet));
        assertEquals(8, service.getAvailableTimeSlots(LocalDate.now().plusDays(1), 1L).size());
        when(appointments.findConflictingAppointments(any(), eq(2L), any(), any())).thenReturn(List.of(new Appointment()));
        assertTrue(service.getAvailableTimeSlots(LocalDate.now().plusDays(1), 1L).isEmpty());
        treatment.setActive(false);
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> service.getAvailableTimeSlots(LocalDate.now().plusDays(1), 1L));
    }

    @Test void completeTransitionMatrix() {
        Map<AppointmentStatus, Set<AppointmentStatus>> allowed = Map.of(
                AppointmentStatus.SCHEDULED, Set.of(AppointmentStatus.CONFIRMED, AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                AppointmentStatus.CONFIRMED, Set.of(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW),
                AppointmentStatus.IN_PROGRESS, Set.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        for (var from : AppointmentStatus.values()) for (var to : AppointmentStatus.values())
            assertEquals(allowed.getOrDefault(from, Set.of()).contains(to), from.canTransitionTo(to), from + " -> " + to);
    }
}
