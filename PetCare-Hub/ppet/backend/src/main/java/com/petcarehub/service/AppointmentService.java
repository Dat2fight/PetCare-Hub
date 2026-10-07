package com.petcarehub.service;

import com.petcarehub.dto.appointment.AppointmentDTO;
import com.petcarehub.dto.appointment.AppointmentCreateRequest;
import com.petcarehub.dto.appointment.AppointmentUpdateRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentService {
    AppointmentDTO bookAppointment(AppointmentCreateRequest request);
    AppointmentDTO updateAppointmentStatus(Long id, AppointmentUpdateRequest request);
    List<AppointmentDTO> getAllAppointments();
    List<AppointmentDTO> getAppointmentsByUser(Long userId);
    List<AppointmentDTO> getAppointmentsByPet(Long petId);
    List<LocalDateTime> getAvailableTimeSlots(LocalDate date, Long serviceId);
}
