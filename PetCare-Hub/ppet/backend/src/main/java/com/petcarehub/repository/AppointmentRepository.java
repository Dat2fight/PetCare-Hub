package com.petcarehub.repository;

import com.petcarehub.entity.Appointment;
import com.petcarehub.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Page<Appointment> findByCustomerId(Long customerId, Pageable pageable);

    Page<Appointment> findByStaffId(Long staffId, Pageable pageable);

    List<Appointment> findByAppointmentDateAndStaffId(LocalDate date, Long staffId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.staff.id = :staffId AND a.status != com.petcarehub.enums.AppointmentStatus.CANCELLED AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findConflictingAppointments(LocalDate date, Long staffId, LocalTime startTime, LocalTime endTime);
}
