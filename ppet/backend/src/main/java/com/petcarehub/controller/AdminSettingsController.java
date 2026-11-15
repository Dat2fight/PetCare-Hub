package com.petcarehub.controller;
import com.petcarehub.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequestMapping("/api/v1/admin/settings") @RequiredArgsConstructor
public class AdminSettingsController {
    private final SettingsService settings;
    @GetMapping @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object current(){
        return settings.current();
    }
    @PutMapping @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void save(@RequestBody JsonNode body){
        settings.update(body);
    }
}
