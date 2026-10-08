package com.petcarehub.controller;

import com.petcarehub.dto.appointment.AppointmentDTO;
import com.petcarehub.dto.appointment.AppointmentCreateRequest;
import com.petcarehub.dto.appointment.AppointmentUpdateRequest;
import com.petcarehub.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByUser(userId));
    }

    @GetMapping("/pet/{petId}")
    public ResponseEntity<List<AppointmentDTO>> getAppointmentsByPet(@PathVariable Long petId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPet(petId));
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> bookAppointment(@Valid @RequestBody AppointmentCreateRequest request) {
        return new ResponseEntity<>(appointmentService.bookAppointment(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentDTO> updateAppointmentStatus(@PathVariable Long id, @RequestBody AppointmentUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, request));
    }

    @GetMapping("/slots")
    public ResponseEntity<List<LocalDateTime>> getAvailableSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, 
            @RequestParam Long serviceId) {
        return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(date, serviceId));
    }
}
