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
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appointment a where a.id = :id and (:admin = true or a.customer.id = :userId or (:staff = true and a.staff.id = :userId))")
    java.util.Optional<Appointment> findAccessibleLockedById(Long id, Long userId, boolean staff, boolean admin);
    Page<Appointment> findByPetIdAndStaffId(Long petId, Long staffId, Pageable pageable);
    boolean existsByCustomerIdAndServiceIdAndStatus(Long customerId, Long serviceId, AppointmentStatus status);
    Page<Appointment> findByPetId(Long petId, Pageable pageable);
    Page<Appointment> findByPetIdAndCustomerId(Long petId, Long customerId, Pageable pageable);
    Page<Appointment> findByCustomerId(Long customerId, Pageable pageable);

    java.util.Optional<Appointment> findByIdAndCustomerId(Long id, Long customerId);

    @Query("""
            from Appointment a where a.customer.id=:customerId and (
              (:filter='cancelled' and a.status=com.petcarehub.enums.AppointmentStatus.CANCELLED) or
              (:filter='upcoming' and a.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED,com.petcarehub.enums.AppointmentStatus.CONFIRMED,com.petcarehub.enums.AppointmentStatus.IN_PROGRESS)
                and (a.appointmentDate>:date or (a.appointmentDate=:date and a.endTime>:time))) or
              (:filter='past' and a.status<>com.petcarehub.enums.AppointmentStatus.CANCELLED and
                (a.status in (com.petcarehub.enums.AppointmentStatus.COMPLETED,com.petcarehub.enums.AppointmentStatus.NO_SHOW)
                  or a.appointmentDate<:date or (a.appointmentDate=:date and a.endTime<=:time))))
            """)
    Page<Appointment> customerSchedule(Long customerId, String filter, LocalDate date, LocalTime time, Pageable pageable);

    Page<Appointment> findByStaffId(Long staffId, Pageable pageable);

    List<Appointment> findByAppointmentDateAndStaffId(LocalDate date, Long staffId);

    List<Appointment> findByStatus(AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.staff.id = :staffId AND a.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED, com.petcarehub.enums.AppointmentStatus.CONFIRMED, com.petcarehub.enums.AppointmentStatus.IN_PROGRESS) AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findConflictingAppointments(LocalDate date, Long staffId, LocalTime startTime, LocalTime endTime);
    @Query("SELECT a FROM Appointment a WHERE a.appointmentDate = :date AND a.pet.id = :petId AND a.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED, com.petcarehub.enums.AppointmentStatus.CONFIRMED, com.petcarehub.enums.AppointmentStatus.IN_PROGRESS) AND a.startTime < :endTime AND a.endTime > :startTime")
    List<Appointment> findPetConflicts(LocalDate date, Long petId, LocalTime startTime, LocalTime endTime);
}
