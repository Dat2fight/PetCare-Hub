package com.petcarehub.controller;
import com.petcarehub.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/admin/catalog") @RequiredArgsConstructor
public class AdminCatalogController  {
    private final AdminCatalogService service;
    private final AdminProductBatch batches;
    @GetMapping("/products/low-stock") @PreAuthorize("@catalogPolicy.read('products')") public Object lowStock() {
        return service.lowStock();
    }
    @GetMapping("/{resource}") @PreAuthorize("@catalogPolicy.read(#resource)") public Object list(@PathVariable String resource,@RequestParam(required=false) String keyword,@RequestParam(required=false) Boolean active,Pageable page) {
        return service.list(resource,keyword,active,page);
    }
    @GetMapping("/{resource}/{id}") @PreAuthorize("@catalogPolicy.read(#resource)") public Object detail(@PathVariable String resource,@PathVariable Long id) {
        return service.detail(resource,id);
    }
    @PostMapping("/{resource}") @PreAuthorize("@catalogPolicy.write(#resource)") public Object create(@PathVariable String resource,@RequestBody JsonNode body) {
        return service.save(resource,null,body);
    }
    @PutMapping("/{resource}/{id}") @PreAuthorize("@catalogPolicy.write(#resource)") public Object edit(@PathVariable String resource,@PathVariable Long id,@RequestBody JsonNode body) {
        return service.save(resource,id,body);
    }
    public record Reason(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=500) String reason) {
    }
    @DeleteMapping("/{resource}/{id}") @PreAuthorize("@catalogPolicy.write(#resource)") public void delete(@PathVariable String resource,@PathVariable Long id,@jakarta.validation.Valid @RequestBody Reason body) {
        service.delete(resource,id,body.reason());
    }
    @PostMapping("/products/bulk-preview") @PreAuthorize("@perm.has('CATALOG_MANAGE')") public Object preview(@RequestBody JsonNode body) {
        return batches.bulk(body,false);
    }
    @PostMapping("/products/bulk-apply") @PreAuthorize("@perm.has('CATALOG_MANAGE')") public Object bulk(@RequestBody JsonNode body) {
        return batches.bulk(body,true);
    }
    @PostMapping("/products/csv-preview") @PreAuthorize("@perm.has('CATALOG_MANAGE')") public Object csvPreview(@RequestBody JsonNode body) {
        return batches.csv(body,false);
    }
    @PostMapping("/products/csv-apply") @PreAuthorize("@perm.has('CATALOG_MANAGE')") public Object csv(@RequestBody JsonNode body) {
        return batches.csv(body,true);
    }
    @PostMapping("/pets/{id}/release-reservation") @PreAuthorize("@perm.has('SALE_PET_MANAGE')") public void release(@PathVariable Long id,@jakarta.validation.Valid @RequestBody Reason body) {
        service.release(id,body.reason());
    }
}
