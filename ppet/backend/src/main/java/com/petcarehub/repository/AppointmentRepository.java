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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id")
    java.util.Optional<Appointment> findLockedById(Long id);
    boolean existsByCustomerIdAndServiceIdAndStatus(Long customerId, Long serviceId, AppointmentStatus status);
    Page<Appointment> findByPetId(Long petId, Pageable pageable);
    Page<Appointment> findByCustomerId(Long customerId, Pageable pageable);

    Page<Appointment> findByStaffId(Long staffId, Pageable pageable);

    List<Appointment> findByAppointmentDateAndStaffId(LocalDate date, Long staffId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.staff.id = :staffId AND a.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED, com.petcarehub.enums.AppointmentStatus.CONFIRMED, com.petcarehub.enums.AppointmentStatus.IN_PROGRESS) AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findConflictingAppointments(LocalDate date, Long staffId, LocalTime startTime, LocalTime endTime);
}
