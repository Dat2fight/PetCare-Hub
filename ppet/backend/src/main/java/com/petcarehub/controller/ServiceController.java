package com.petcarehub.controller;

import com.petcarehub.dto.service.ServiceCreateRequest;
import com.petcarehub.dto.service.ServiceEntityDTO;
import com.petcarehub.dto.service.ServiceUpdateRequest;
import com.petcarehub.enums.ServiceType;
import com.petcarehub.service.ServiceManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceManagementService serviceManagementService;

    @GetMapping
    public ResponseEntity<List<ServiceEntityDTO>> getAllServices(
            @RequestParam(required = false) ServiceType type) {
        log.info("Fetching services - type filter: {}", type);
        if (type != null) {
            return ResponseEntity.ok(serviceManagementService.getServicesByType(type));
        }
        return ResponseEntity.ok(serviceManagementService.getAllServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceEntityDTO> getServiceById(@PathVariable Long id) {
        log.info("Fetching service by id: {}", id);
        return ResponseEntity.ok(serviceManagementService.getServiceById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ServiceEntityDTO> createService(@Valid @RequestBody ServiceCreateRequest request) {
        ServiceEntityDTO createdService = serviceManagementService.createService(request);
        return new ResponseEntity<>(createdService, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ServiceEntityDTO> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceUpdateRequest request) {
        return ResponseEntity.ok(serviceManagementService.updateService(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceManagementService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}
