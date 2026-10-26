package com.petcarehub.service.impl;

import com.petcarehub.dto.appointment.AppointmentDTO;
import com.petcarehub.dto.appointment.AppointmentCreateRequest;
import com.petcarehub.dto.appointment.AppointmentUpdateRequest;
import com.petcarehub.service.AppointmentService;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.enums.AppointmentStatus;
import com.petcarehub.enums.RoleType;
import com.petcarehub.entity.Appointment;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.ServiceEntity;
import com.petcarehub.entity.User;
import com.petcarehub.repository.AppointmentRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.UserRepository;
import com.petcarehub.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;
    private final com.petcarehub.config.BookingProperties booking;
    private final com.petcarehub.security.Ownership ownership;
    
    @Override
    @Transactional
    public AppointmentDTO bookAppointment(AppointmentCreateRequest request) {
        Long currentUserId = ownership.userId();
        User customer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Pet pet = ownership.ownedPet(request.getPetId());
        ServiceEntity serviceEntity = requireService(request.getServiceId());
        LocalDateTime start = request.getAppointmentTime();
        int duration = serviceEntity.getDurationMinutes() == null ? 0 : serviceEntity.getDurationMinutes();
        if (duration < 5 || duration > 480 || start == null || !start.isAfter(LocalDateTime.now().plusMinutes(booking.getMinimumNoticeMinutes())))
            throw new BadRequestException("Thời gian đặt lịch hoặc thời lượng không hợp lệ");
        LocalDateTime end = start.plusMinutes(duration);
        if (start.toLocalTime().isBefore(booking.getOpeningTime()) || !start.toLocalDate().equals(end.toLocalDate()) || end.toLocalTime().isAfter(booking.getClosingTime()))
            throw new BadRequestException("Lịch hẹn phải nằm trong giờ làm việc");
        LocalDate appointmentDate = start.toLocalDate();
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        User staff = null;
        List<User> candidates = request.getStaffId() == null ? userRepository.findEligibleStaff(serviceEntity.getServiceType().staffRole())
                : List.of(userRepository.findById(request.getStaffId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên")));
        for (User candidate : candidates) {
            // Lock a stable resource row even when there are no existing appointments.
            User locked = userRepository.findLockedById(candidate.getId()).orElseThrow();
            if (eligible(locked, serviceEntity) && appointmentRepository.findConflictingAppointments(appointmentDate, locked.getId(), startTime, endTime).isEmpty()) {
                staff = locked; break;
            }
        }
        if (staff == null) throw new IllegalStateException("Không có nhân viên phù hợp");

        Appointment appointment = new Appointment();
        appointment.setPet(pet);
        appointment.setService(serviceEntity);
        appointment.setStaff(staff);
        appointment.setCustomer(customer);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setNotes(request.getNotes());
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        
        appointment = appointmentRepository.save(appointment);
        return mapToDTO(appointment);
    }
    
    @Override
    @Transactional
    public AppointmentDTO updateAppointmentStatus(Long id, AppointmentUpdateRequest request) {
        Long currentUserId = ownership.userId();
        Appointment appointment = appointmentRepository.findAccessibleLockedById(id, currentUserId,
                ownership.hasRole("SALES_STAFF") || ownership.hasRole("VETERINARIAN"), ownership.hasRole("ADMIN"))
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn"));
        boolean staff = ownership.hasRole("ADMIN")
                || ((ownership.hasRole("SALES_STAFF") || ownership.hasRole("VETERINARIAN"))
                    && appointment.getStaff() != null && appointment.getStaff().getId().equals(currentUserId));
        if (!staff && (appointment.getCustomer() == null || !appointment.getCustomer().getId().equals(currentUserId)
                || request.getStatus() != AppointmentStatus.CANCELLED
                || !LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime()).isAfter(LocalDateTime.now())))
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền đổi trạng thái lịch hẹn");
        if (request.getStatus() != null) {
            if (!appointment.getStatus().canTransitionTo(request.getStatus())) throw new IllegalStateException("Không thể chuyển trạng thái lịch hẹn");
            appointment.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            appointment.setNotes(request.getNotes());
        }
        
        appointment = appointmentRepository.save(appointment);
        return mapToDTO(appointment);
    }
    
    @Override
    @Transactional(readOnly = true)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'SALES_STAFF', 'VETERINARIAN')")
    public List<AppointmentDTO> getAllAppointments(org.springframework.data.domain.Pageable pageable) {
        var visible = ownership.hasRole("ADMIN") ? appointmentRepository.findAll(pageable)
                : appointmentRepository.findByStaffId(ownership.userId(), pageable);
        return visible.stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByUser(Long userId, org.springframework.data.domain.Pageable pageable) {
        ownership.requireUser(userId);
        return appointmentRepository.findByCustomerId(userId, pageable)
            .getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByPet(Long petId, org.springframework.data.domain.Pageable pageable) {
        ownership.requirePet(petId);
        // A pet may change owners; customers must not see a previous owner's appointments.
        var appointments = ownership.hasRole("ADMIN")
                ? appointmentRepository.findByPetId(petId, pageable)
                : ownership.hasRole("VETERINARIAN") ? appointmentRepository.findByPetIdAndStaffId(petId, ownership.userId(), pageable)
                : appointmentRepository.findByPetIdAndCustomerId(petId, ownership.userId(), pageable);
        return appointments.stream().map(this::mapToDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlots(LocalDate date, Long serviceId) {
        ServiceEntity service = requireService(serviceId);
        int duration = service.getDurationMinutes() == null ? 0 : service.getDurationMinutes();
        if (duration < 5 || duration > 480) return List.of();
        List<User> staff = userRepository.findEligibleStaff(service.getServiceType().staffRole());
        List<LocalDateTime> slots = new ArrayList<>();
        LocalDateTime close = date.atTime(booking.getClosingTime());
        LocalDateTime now = LocalDateTime.now().plusMinutes(booking.getMinimumNoticeMinutes());
        for (LocalDateTime start = date.atTime(booking.getOpeningTime()); !start.plusMinutes(duration).isAfter(close); start = start.plusMinutes(duration)) {
            if (!start.isAfter(now)) continue;
            for (User candidate : staff) {
                if (eligible(candidate, service) && appointmentRepository.findConflictingAppointments(date, candidate.getId(), start.toLocalTime(), start.plusMinutes(duration).toLocalTime()).isEmpty()) {
                    slots.add(start); break;
                }
            }
        }
        return slots;
    }

    private ServiceEntity requireService(Long id) {
        ServiceEntity service = entityManager.find(ServiceEntity.class, id);
        if (service == null) throw new ResourceNotFoundException("Không tìm thấy dịch vụ");
        if (!Boolean.TRUE.equals(service.getActive())) throw new BadRequestException("Dịch vụ đã ngừng hoạt động");
        return service;
    }

    private boolean eligible(User staff, ServiceEntity service) {
        return Boolean.TRUE.equals(staff.getEnabled()) && staff.getRoles().stream().anyMatch(r -> r.getName() == service.getServiceType().staffRole());
    }

    private AppointmentDTO mapToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPetId(appointment.getPet() != null ? appointment.getPet().getId() : null);
        dto.setServiceId(appointment.getService() != null ? appointment.getService().getId() : null);
        dto.setStaffId(appointment.getStaff() != null ? appointment.getStaff().getId() : null);
        dto.setCustomerId(appointment.getCustomer() != null ? appointment.getCustomer().getId() : null);
        dto.setAppointmentTime(LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime()));
        dto.setStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null);
        dto.setNotes(appointment.getNotes());
        return dto;
    }
}
