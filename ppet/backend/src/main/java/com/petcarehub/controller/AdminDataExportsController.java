package com.petcarehub.controller;
import com.petcarehub.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
@RestController @RequestMapping("/api/v1/admin/data-exports") @RequiredArgsConstructor
public class AdminDataExportsController {
    private final DataExportsService service;
    @GetMapping @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object list(Pageable p){
        return service.history(p);
    }
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED) @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public Object create(@RequestBody JsonNode body){
        return service.create(body);
    }
    @PostMapping("/{id}/download") @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public ResponseEntity<StreamingResponseBody> download(@PathVariable long id){
        var r=service.download(id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(r.filename()).build().toString()).contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(r.body());
    }
}
