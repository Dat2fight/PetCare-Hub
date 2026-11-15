package com.petcarehub.controller;
import com.petcarehub.service.SiteContentService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/admin/content") @RequiredArgsConstructor
public class AdminContentController {
    private final SiteContentService service;
    @GetMapping @PreAuthorize("@perm.has('CONTENT_MANAGE')") public Object list(@RequestParam(required=false)String kind,@RequestParam(required=false)String keyword,Pageable p){
        return service.list(kind,keyword,p);
    }
    @GetMapping("/{id}") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public Object detail(@PathVariable long id){
        return service.detail(id);
    }
    @PostMapping @PreAuthorize("@perm.has('CONTENT_MANAGE')") public Object create(@RequestBody JsonNode body){
        return service.save(null,body);
    }
    @PutMapping("/{id}") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public Object save(@PathVariable long id,@RequestBody JsonNode body){
        return service.save(id,body);
    }
    @PostMapping("/{id}/publish") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void publish(@PathVariable long id){
        service.publish(id,true);
    }
    @PostMapping("/{id}/unpublish") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void unpublish(@PathVariable long id){
        service.publish(id,false);
    }
    @GetMapping("/{id}/revisions") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public Object revisions(@PathVariable long id,Pageable p){
        return service.revisions(id,p);
    }
    public record Restore(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long revisionId){
    }
    @PostMapping("/{id}/restore") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void restore(@PathVariable long id,@jakarta.validation.Valid @RequestBody Restore body){
        service.restore(id,body.revisionId());
    }
    @DeleteMapping("/{id}") @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void delete(@PathVariable long id,@jakarta.validation.Valid @RequestBody AdminCatalogController.Reason body){
        service.delete(id,body.reason());
    }
}
