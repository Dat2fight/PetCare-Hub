package com.petcarehub.controller;
import com.petcarehub.service.ReturnsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/returns") @RequiredArgsConstructor
public class CustomerReturnsController {
    private final ReturnsService service;
    @GetMapping @PreAuthorize("hasRole('CUSTOMER')") public Object mine(Pageable p){
        return service.mine(p);
    }
    @PostMapping @PreAuthorize("hasRole('CUSTOMER')") public Object create(@RequestBody JsonNode body){
        return service.create(body);
    }
}
