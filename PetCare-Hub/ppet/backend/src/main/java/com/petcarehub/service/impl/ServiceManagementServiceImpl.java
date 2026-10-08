package com.petcarehub.service.impl;

import com.petcarehub.dto.service.ServiceCreateRequest;
import com.petcarehub.dto.service.ServiceEntityDTO;
import com.petcarehub.dto.service.ServiceUpdateRequest;
import com.petcarehub.entity.ServiceEntity;
import com.petcarehub.enums.ServiceType;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.ServiceEntityRepository;
import com.petcarehub.service.ServiceManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceManagementServiceImpl implements ServiceManagementService {

    private final ServiceEntityRepository serviceEntityRepository;

    @Override
    @Transactional
    public ServiceEntityDTO createService(ServiceCreateRequest request) {
        ServiceEntity entity = ServiceEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .serviceType(request.getServiceType())
                .durationMinutes(request.getDurationMinutes())
                .price(request.getPrice())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        
        entity = serviceEntityRepository.save(entity);
        return mapToDTO(entity);
    }

    @Override
    @Transactional
    public ServiceEntityDTO updateService(Long id, ServiceUpdateRequest request) {
        ServiceEntity entity = getServiceEntity(id);

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setServiceType(request.getServiceType());
        entity.setDurationMinutes(request.getDurationMinutes());
        entity.setPrice(request.getPrice());
        entity.setActive(request.getActive());

        entity = serviceEntityRepository.save(entity);
        return mapToDTO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceEntityDTO getServiceById(Long id) {
        return mapToDTO(getServiceEntity(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceEntityDTO> getAllServices() {
        return serviceEntityRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceEntityDTO> getServicesByType(ServiceType type) {
        return serviceEntityRepository.findByServiceType(type).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        ServiceEntity entity = getServiceEntity(id);
        serviceEntityRepository.delete(entity);
    }

    private ServiceEntity getServiceEntity(Long id) {
        return serviceEntityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
    }

    private ServiceEntityDTO mapToDTO(ServiceEntity entity) {
        return ServiceEntityDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .serviceType(entity.getServiceType())
                .durationMinutes(entity.getDurationMinutes())
                .price(entity.getPrice())
                .active(entity.getActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
