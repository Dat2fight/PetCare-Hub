package com.petcarehub.controller;
import com.petcarehub.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminCatalogExtrasController  {
    private final AdminCatalogService catalog;
    private final AdminScheduleService schedule;
    private final AdminMarketingService marketing;
    @GetMapping("/pet-support/{id}") @PreAuthorize("@perm.has('USER_MANAGE') or @perm.has('ASSIGNED_MEDICAL_MANAGE')") public Object support(@PathVariable Long id) {
        return catalog.support(id);
    }
    @GetMapping("/schedules/{kind}") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public Object schedule(@PathVariable String kind,@RequestParam(required=false) Long staff,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,Pageable page) {
        return schedule.list(kind,staff,from,to,page);
    }
    @PostMapping("/schedules/{kind}") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public Object scheduleCreate(@PathVariable String kind,@RequestBody JsonNode body) {
        return schedule.save(kind,null,body);
    }
    @PutMapping("/schedules/{kind}/{id}") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public Object scheduleEdit(@PathVariable String kind,@PathVariable Long id,@RequestBody JsonNode body) {
        return schedule.save(kind,id,body);
    }
    @DeleteMapping("/schedules/{kind}/{id}") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public void scheduleDelete(@PathVariable String kind,@PathVariable Long id,@jakarta.validation.Valid @RequestBody AdminCatalogController.Reason body) {
        schedule.delete(kind,id,body.reason());
    }
    @GetMapping("/schedule-affected") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public Object affected(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestParam(required=false) Long staff,Pageable page) {
        return schedule.affected(date,staff,page);
    }
    public record Cancel(LocalDate date,List<Long> ids,String reason) {
    }
    @PostMapping("/schedule-cancel") @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") public void cancel(@RequestBody Cancel body) {
        schedule.cancelAffected(body.date(),body.ids(),body.reason());
    }
    @GetMapping("/staff-performance") @PreAuthorize("@perm.has('REPORT_VIEW')") public Object performance(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,Pageable page) {
        return schedule.performance(from,to,page);
    }
    @GetMapping("/loyalty-rules") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object rules() {
        return marketing.rules();
    }
    @PutMapping("/loyalty-rules") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void rules(@RequestBody JsonNode body) {
        marketing.rules(body);
    }
    @PostMapping("/loyalty/{userId}/adjust") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void adjust(@PathVariable Long userId,@RequestBody JsonNode body) {
        marketing.adjust(userId,body);
    }
    @GetMapping("/promotion-usages/{id}") @PreAuthorize("@perm.has('PROMOTION_MANAGE')") public Object usages(@PathVariable Long id,Pageable page) {
        return marketing.usages(id,page);
    }
    @GetMapping("/subscriptions") @PreAuthorize("@perm.has('PACKAGE_MANAGE')") public Object subscriptions(@RequestParam(required=false) Long packageId,Pageable page) {
        return marketing.subscriptions(packageId,page);
    }
    @PostMapping("/subscriptions/{id}") @PreAuthorize("@perm.has('PACKAGE_MANAGE')") public void subscription(@PathVariable Long id,@RequestBody JsonNode body) {
        marketing.subscription(id,body);
    }
    @GetMapping("/campaign-audience") @PreAuthorize("@perm.has('PROMOTION_MANAGE')") public Object audience(@RequestParam String group,@RequestParam(defaultValue="90") int days) {
        return marketing.audiencePreview(group,days);
    }
    @PostMapping("/campaigns") @PreAuthorize("@perm.has('PROMOTION_MANAGE')") public Object campaign(@RequestBody JsonNode body) {
        return marketing.campaign(body);
    }
}
