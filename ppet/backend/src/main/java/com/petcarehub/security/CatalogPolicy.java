package com.petcarehub.security;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.authorization.*;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
@Component("catalogPolicy") @RequiredArgsConstructor
public class CatalogPolicy  {
    private final PermissionService perm;
    public static final Map<String,Permission> WRITES=Map.of("products",Permission.CATALOG_MANAGE,"categories",Permission.CATALOG_MANAGE,"pets",Permission.SALE_PET_MANAGE,"species",Permission.SALE_PET_MANAGE,"breeds",Permission.SALE_PET_MANAGE,"services",Permission.CATALOG_MANAGE,"promotions",Permission.PROMOTION_MANAGE,"packages",Permission.PACKAGE_MANAGE);
    public boolean write(String resource) {
        return WRITES.containsKey(resource)&&perm.has(WRITES.get(resource).name());
    }
    public boolean read(String resource) {
        return write(resource)||Set.of("products","categories").contains(resource)&&perm.has("CATALOG_READ");
    }
    public AuthorizationManager<RequestAuthorizationContext> access() {
        return (auth,context)-> {
            String[] path=context.getRequest().getRequestURI().split("/");
            String resource=path.length>5?path[5]:"";
            return new AuthorizationDecision("GET".equals(context.getRequest().getMethod())?read(resource):write(resource));
        }
        ;
    }
}
