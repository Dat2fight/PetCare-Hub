package com.petcarehub.controller;
import com.petcarehub.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor
public class AdminInsightsController {
    private final AdminInsightsService insights;
    private final AdminCsvExport exports;
    private final AdminSystemService system;
    @GetMapping("/dashboard/summary") @PreAuthorize("@perm.has('REPORT_VIEW')") public Object summary(@RequestParam(defaultValue="month") String period,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
        return insights.summary(period,from,to);
    }
    @GetMapping("/dashboard/alerts") @PreAuthorize("@perm.has('REPORT_VIEW')") public Object alerts(){
        return insights.alerts();
    }
    @GetMapping("/reports/revenue") @PreAuthorize("@perm.has('REPORT_VIEW')") public Object revenue(@RequestParam(defaultValue="day") String granularity,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
        return insights.revenue(granularity,new ReportPeriod(from,to));
    }
    @GetMapping("/reports/rankings") @PreAuthorize("@perm.has('REPORT_VIEW')") public Object rankings(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){
        return insights.rankings(new ReportPeriod(from,to));
    }
    @GetMapping("/reports/export") @PreAuthorize("@perm.has('REPORT_VIEW')") public ResponseEntity<StreamingResponseBody> export(@RequestParam String kind,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) String keyword){
        var result=exports.export(kind,from,to,keyword);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(result.filename()).build().toString()).contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(result.body());
    }
    @GetMapping("/system/status") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object system(){
        return system.status();
    }
}
