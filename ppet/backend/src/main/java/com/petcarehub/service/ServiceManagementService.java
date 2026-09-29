package com.petcarehub.service;

import com.petcarehub.dto.service.ServiceCreateRequest;
import com.petcarehub.dto.service.ServiceEntityDTO;
import com.petcarehub.dto.service.ServiceUpdateRequest;
import com.petcarehub.enums.ServiceType;

import java.util.List;

public interface ServiceManagementService {
    ServiceEntityDTO createService(ServiceCreateRequest request);
    ServiceEntityDTO updateService(Long id, ServiceUpdateRequest request);
    ServiceEntityDTO getServiceById(Long id);
    List<ServiceEntityDTO> getAllServices();
    List<ServiceEntityDTO> getServicesByType(ServiceType type);
    void deleteService(Long id);
}
