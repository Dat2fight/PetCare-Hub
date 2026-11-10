package com.petcarehub.controller;

import com.petcarehub.entity.CarePackage;
import com.petcarehub.enums.SubscriptionStatus;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/care-packages") @RequiredArgsConstructor
@Transactional(readOnly=true)
public class CarePackageController {
    private final CarePackageRepository plans;
    private final CarePackageSubscriptionRepository subscriptions;
    private final Ownership ownership;
    private final Clock clock;
    static final String STAFF="hasAnyRole('ADMIN','SALES_STAFF')";
    public record Changes(@NotBlank @Size(max=255) String name, @Size(max=4000) String description,
            @NotNull @Positive @Digits(integer=8,fraction=2) BigDecimal price, @NotNull @Min(1) @Max(120) Integer durationMonths,
            boolean includesGrooming, boolean includesCheckup, boolean includesVaccination, @NotNull Boolean active) {
        void apply(CarePackage p) {
            p.setName(name.strip()); p.setDescription(description); p.setPrice(price); p.setDurationMonths(durationMonths);
            p.setIncludesGrooming(includesGrooming); p.setIncludesCheckup(includesCheckup);
            p.setIncludesVaccination(includesVaccination); p.setActive(active);
        }
    }
    public record Subscription(Long id,Long orderId,String name,String petName,LocalDate startDate,LocalDate endDate,
            SubscriptionStatus status,boolean includesGrooming,boolean includesCheckup,boolean includesVaccination) {}

    @GetMapping public List<CarePackage> list() { return plans.findByActiveTrue(); }
    @GetMapping("/{id}") public CarePackage detail(@PathVariable Long id) {
        return plans.findById(id).filter(p -> Boolean.TRUE.equals(p.getActive())).orElseThrow(this::missing);
    }
    @GetMapping("/manage") @PreAuthorize(STAFF)
    public List<CarePackage> manage() { return plans.findAll(); }
    @PostMapping @Transactional @PreAuthorize(STAFF) @ResponseStatus(HttpStatus.CREATED)
    public CarePackage create(@Valid @RequestBody Changes changes) {
        var p=new CarePackage(); changes.apply(p); return plans.save(p);
    }
    @PutMapping("/{id}") @Transactional @PreAuthorize(STAFF)
    public CarePackage edit(@PathVariable Long id,@Valid @RequestBody Changes changes) {
        var p=plans.findLockedById(id).orElseThrow(this::missing); changes.apply(p); return p;
    }
    @DeleteMapping("/{id}") @Transactional @PreAuthorize(STAFF) @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) { plans.findLockedById(id).orElseThrow(this::missing).setActive(false); }
    @GetMapping("/subscriptions/me") @PreAuthorize("hasRole('CUSTOMER')")
    public List<Subscription> mine() {
        var today=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        return subscriptions.findByCustomerIdOrderByIdDesc(ownership.userId()).stream().map(s -> {
            var p=s.getCarePackage(); var line=s.getOrderItem();
            var status=s.getStatus()==SubscriptionStatus.ACTIVE && today.isAfter(s.getEndDate()) ? SubscriptionStatus.EXPIRED : s.getStatus();
            return new Subscription(s.getId(),line==null ? null : line.getOrder().getId(),line==null ? p.getName() : line.getPackageName(),
                    s.getPet().getName(),s.getStartDate(),s.getEndDate(),status,line==null ? p.getIncludesGrooming() : line.getPackageGrooming(),
                    line==null ? p.getIncludesCheckup() : line.getPackageCheckup(),line==null ? p.getIncludesVaccination() : line.getPackageVaccination());
        }).toList();
    }
    private ResourceNotFoundException missing() { return new ResourceNotFoundException("Không tìm thấy gói chăm sóc"); }
}
