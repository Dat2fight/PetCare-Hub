package com.petcarehub.controller;
import com.petcarehub.service.ReturnsService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/admin/returns") @RequiredArgsConstructor
public class AdminReturnsController {
    private final ReturnsService service;
    @GetMapping @PreAuthorize("@perm.has('ORDER_MANAGE')") public Object list(@RequestParam(required=false)String status,Pageable p){
        return service.queue(status,p);
    }
    @PostMapping("/{id}/transition") @PreAuthorize("@perm.has('ORDER_MANAGE')") public void transition(@PathVariable long id,@RequestBody JsonNode body){
        service.transition(id,body);
    }
}
