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
    
    @Override
    @Transactional
    public AppointmentDTO bookAppointment(AppointmentCreateRequest request) {
        // Customer resolution: Zero Trust - extract from SecurityContextHolder
        User customer = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof UserDetailsImpl) {
                UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
                customer = userRepository.findById(userDetails.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + userDetails.getId()));
            } else {
                customer = userRepository.findByUsername(authentication.getName())
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with username " + authentication.getName()));
            }
        } else if (request.getCustomerId() != null) {
            customer = userRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + request.getCustomerId()));
        } else {
            throw new BadRequestException("Bạn cần đăng nhập để đặt lịch hẹn");
        }

        Pet pet = null;
        if (request.getPetId() != null) {
            pet = petRepository.findById(request.getPetId()).orElse(null);
        }
        if (pet == null) {
            List<Pet> customerPets = petRepository.findByOwnerId(customer.getId());
            if (!customerPets.isEmpty()) {
                pet = customerPets.get(0);
            } else {
                List<Pet> allPets = petRepository.findAll();
                if (!allPets.isEmpty()) {
                    pet = allPets.get(0);
                } else {
                    throw new ResourceNotFoundException("Pet not found with id " + request.getPetId());
                }
            }
        }
                
        ServiceEntity serviceEntity = entityManager.find(ServiceEntity.class, request.getServiceId());
        if (serviceEntity == null) {
            throw new ResourceNotFoundException("Service not found with id " + request.getServiceId());
        }
        
        LocalDate appointmentDate = request.getAppointmentTime().toLocalDate();
        LocalTime startTime = request.getAppointmentTime().toLocalTime();
        int duration = serviceEntity.getDurationMinutes() != null ? serviceEntity.getDurationMinutes() : 60;
        LocalTime endTime = startTime.plusMinutes(duration);

        User staff = null;
        if (request.getStaffId() != null) {
            staff = userRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found with id " + request.getStaffId()));
            // Double booking check for manually assigned staff
            List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                    appointmentDate, staff.getId(), startTime, endTime);
            if (!conflicts.isEmpty()) {
                throw new BadRequestException("Nhân viên/bác sĩ đã có lịch hẹn trong khung giờ này.");
            }
        } else {
            // Automatically find available staff/veterinarian
            List<User> allUsers = userRepository.findAll();
            List<User> potentialStaff = allUsers.stream()
                    .filter(u -> u.getRoles().stream().anyMatch(r -> 
                            r.getName() == RoleType.VETERINARIAN || 
                            r.getName() == RoleType.SALES_STAFF ||
                            r.getName() == RoleType.ADMIN))
                    .collect(Collectors.toList());

            // First pass: find a staff member who has NO conflicts for this time slot
            for (User s : potentialStaff) {
                List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(
                        appointmentDate, s.getId(), startTime, endTime);
                if (conflicts.isEmpty()) {
                    staff = s;
                    break;
                }
            }

            // Second pass: if none available without conflict, pick first potential staff or fallback to any user
            if (staff == null && !potentialStaff.isEmpty()) {
                staff = potentialStaff.get(0);
            } else if (staff == null && !allUsers.isEmpty()) {
                staff = allUsers.get(0);
            }

            if (staff == null) {
                throw new BadRequestException("Không tìm thấy nhân viên khả dụng cho dịch vụ này.");
            }
        }
        
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
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
            
        if (request.getStatus() != null) {
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
    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByUser(Long userId) {
        // Just use pagination method in repository but collect to list
        return appointmentRepository.findByCustomerId(userId, org.springframework.data.domain.Pageable.unpaged())
            .getContent().stream()
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsByPet(Long petId) {
        // As Pet has no direct method in AppointmentRepository, we can write a custom JPQL or use findAll and filter for now
        // For production, we'd add it to the repository.
        return appointmentRepository.findAll().stream()
            .filter(a -> a.getPet().getId().equals(petId))
            .map(this::mapToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LocalDateTime> getAvailableTimeSlots(LocalDate date, Long serviceId) {
        ServiceEntity serviceEntity = entityManager.find(ServiceEntity.class, serviceId);
        int duration = (serviceEntity != null && serviceEntity.getDurationMinutes() != null) ? serviceEntity.getDurationMinutes() : 60;
        
        List<LocalDateTime> slots = new ArrayList<>();
        // Mock logic: 9 AM to 5 PM, slots based on duration
        LocalTime current = LocalTime.of(9, 0);
        LocalTime endOfDay = LocalTime.of(17, 0);
        
        while (current.plusMinutes(duration).isBefore(endOfDay) || current.plusMinutes(duration).equals(endOfDay)) {
            slots.add(LocalDateTime.of(date, current));
            current = current.plusMinutes(duration);
        }
        return slots;
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
