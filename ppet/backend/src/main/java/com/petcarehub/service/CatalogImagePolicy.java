package com.petcarehub.service;
import java.net.URI;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class CatalogImagePolicy  {
    @Value("${app.catalog.image-hosts:images.unsplash.com,images.pexels.com}") private String hosts;
    public String check(String value) {
        if(value==null||value.isBlank())return null;
        try {
            var uri=URI.create(value);
            if(value.length()>500||!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getPort()!=-1&&uri.getPort()!=443||Arrays.stream(hosts.split(",")).map(String::strip).noneMatch(h->h.equalsIgnoreCase(uri.getHost())))throw new IllegalArgumentException();
            return value;
        }
        catch(IllegalArgumentException e) {
            throw CatalogInput.bad("Ảnh phải là URL HTTPS thuộc tên miền được cấu hình");
        }
    }
}
