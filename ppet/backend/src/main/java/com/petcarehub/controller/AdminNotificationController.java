package com.petcarehub.controller;
import com.petcarehub.service.AdminNotificationService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/admin/notifications") @RequiredArgsConstructor
public class AdminNotificationController {
    private final AdminNotificationService service;
    @GetMapping("/audience-preview") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object preview(@RequestParam String group,@RequestParam(required=false) Long userId,@RequestParam(required=false) String role,@RequestParam(required=false) String tier,@RequestParam(defaultValue="90") int days){
        return service.preview(new AdminNotificationService.Audience(group,userId,role,tier,days));
    }
    @PostMapping("/send") @ResponseStatus(org.springframework.http.HttpStatus.ACCEPTED) @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object send(@RequestBody JsonNode body){
        return service.send(body);
    }
    @GetMapping("/history") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object history(Pageable page){
        return service.history(page);
    }
    @GetMapping("/templates") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object templates(Pageable page){
        return service.templates(page);
    }
    @PostMapping("/templates") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object create(@RequestBody JsonNode body){
        return service.template(null,body);
    }
    @PutMapping("/templates/{id}") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object edit(@PathVariable Long id,@RequestBody JsonNode body){
        return service.template(id,body);
    }
    @DeleteMapping("/templates/{id}") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void delete(@PathVariable Long id,@jakarta.validation.Valid @RequestBody AdminCatalogController.Reason body){
        service.deleteTemplate(id,body.reason());
    }
}
