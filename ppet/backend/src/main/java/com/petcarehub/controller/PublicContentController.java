package com.petcarehub.controller;
import com.petcarehub.service.SiteContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
@RestController @RequestMapping("/api/v1/content") @RequiredArgsConstructor
public class PublicContentController {
    private final SiteContentService service;
    @GetMapping public Object list(@RequestParam(defaultValue="ARTICLE") String kind,Pageable p){
        return service.publicList(kind,p);
    }
    @GetMapping("/featured") public Object featured(){
        return service.featured();
    }
    @GetMapping("/{slug}") public Object detail(@PathVariable String slug){
        return service.publicDetail(slug);
    }
}
