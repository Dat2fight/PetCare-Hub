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
    
    @Override
    @Transactional
    public AppointmentDTO bookAppointment(AppointmentCreateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl principal))
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        User customer = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng"));
        if (pet.getOwner() == null || !pet.getOwner().getId().equals(customer.getId()))
            throw new org.springframework.security.access.AccessDeniedException("Bạn chỉ được đặt lịch cho thú cưng của mình");
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
        Appointment appointment = appointmentRepository.findLockedById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl principal))
            throw new org.springframework.security.authentication.InsufficientAuthenticationException("Bạn cần đăng nhập");
        boolean staff = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                || (appointment.getStaff() != null && appointment.getStaff().getId().equals(principal.getId()));
        if (!staff && (appointment.getCustomer() == null || !appointment.getCustomer().getId().equals(principal.getId())
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
    public List<AppointmentDTO> getAllAppointments(org.springframework.data.domain.Pageable pageable) {
        return appointmentRepository.findAll(pageable).stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByUser(Long userId, org.springframework.data.domain.Pageable pageable) {
        // Just use pagination method in repository but collect to list
        return appointmentRepository.findByCustomerId(userId, pageable)
            .getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByPet(Long petId, org.springframework.data.domain.Pageable pageable) {
        return appointmentRepository.findByPetId(petId, pageable).stream()
                .map(this::mapToDTO).toList();
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
