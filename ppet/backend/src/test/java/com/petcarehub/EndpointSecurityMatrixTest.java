package com.petcarehub;

import com.petcarehub.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/** Explicit inventory: adding any application endpoint requires a reviewed policy here. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class EndpointSecurityMatrixTest {
    @Autowired MockMvc mvc;
    @Autowired RequestMappingHandlerMapping mappings;
    // Only public authentication and storefront reads; /manage is deliberately excluded.
    static final Set<String> PUBLIC = Set.of(
        "POST /auth/login", "POST /auth/register", "GET /pets", "GET /pets/{id}",
        "GET /products", "GET /products/{id}", "GET /categories", "GET /categories/{id}",
        "GET /services", "GET /services/{id}", "GET /species", "GET /species/{speciesId}/breeds",
        "GET /promotions", "GET /promotions/{id}", "GET /promotions/validate/{code}",
        "GET /reviews", "GET /reviews/{id}");
    // Customer operations must obtain identity from the principal and enforce resource ownership.
    static final Set<String> CUSTOMER_WRITES = Set.of(
        "POST /cart/items", "PUT /cart/items/{itemId}", "DELETE /cart/items/{itemId}", "DELETE /cart",
        "POST /orders", "POST /orders/{id}/cancel", "POST /appointments", "PUT /appointments/{id}/status",
        "POST /reviews", "PUT /users/me", "POST /pets/my-pets", "PUT /pets/my-pets/{id}",
        "PUT /notifications/{id}/read", "PUT /notifications/read-all",
        "POST /pet-matching/submit", "POST /pet-matching/submit/{userId}");
    static final Set<String> STAFF_WRITES = Set.of(
        "POST /products", "PUT /products/{id}", "DELETE /products/{id}",
        "POST /categories", "PUT /categories/{id}", "DELETE /categories/{id}",
        "POST /promotions", "PUT /promotions/{id}", "DELETE /promotions/{id}",
        "POST /services", "PUT /services/{id}", "DELETE /services/{id}",
        "POST /pets", "PUT /pets/{id}", "DELETE /pets/{id}",
        "POST /medical-records", "PUT /medical-records/{id}", "DELETE /medical-records/{id}",
        "POST /vaccinations", "PUT /vaccinations/{id}", "DELETE /vaccinations/{id}",
        "POST /treatments", "PUT /treatments/{id}", "DELETE /treatments/{id}",
        "POST /inventory/add", "POST /inventory/reduce", "POST /inventory/adjust",
        "PUT /reviews/{id}/approve", "POST /admin/staff", "PATCH /admin/users/{userId}/role",
        "POST /orders/{id}/confirm", "POST /orders/{id}/process", "POST /orders/{id}/ship", "POST /orders/{id}/deliver",
        "POST /loyalty/account/{userId}/add", "POST /loyalty/account/spend", "POST /loyalty/account/{userId}/spend");
    static final Set<String> PRIVATE_READS = Set.of(
        "GET /auth/me", "GET /cart", "GET /orders", "GET /orders/{id}",
        "GET /pets/my-pets", "GET /pets/my-pets/{id}", "GET /products/manage",
        "GET /appointments", "GET /appointments/me", "GET /appointments/user/{userId}", "GET /appointments/pet/{petId}", "GET /appointments/slots",
        "GET /medical-records/{id}", "GET /medical-records/pet/{petId}",
        "GET /vaccinations/{id}", "GET /vaccinations/pet/{petId}",
        "GET /treatments/{id}", "GET /treatments/record/{recordId}",
        "GET /notifications", "GET /notifications/{userId}", "GET /notifications/unread-count",
        "GET /inventory/{productId}", "GET /inventory/low-stock", "GET /inventory/{inventoryId}/transactions",
        "GET /loyalty/account", "GET /loyalty/transactions", "GET /loyalty/account/{userId}", "GET /loyalty/account/{accountId}/transactions",
        "GET /admin/dashboard", "GET /admin/revenue-report", "GET /admin/users", "GET /health");

    @Test void everyMappedEndpointHasAnExplicitPolicyAndAnonymousBoundary() throws Exception {
        Set<String> expected=new TreeSet<>();
        for(var group:List.of(PUBLIC,CUSTOMER_WRITES,STAFF_WRITES,PRIVATE_READS)) expected.addAll(group);
        Set<String> actual=new TreeSet<>();
        for(var entry:mappings.getHandlerMethods().entrySet()) {
            if(!entry.getValue().getBeanType().getPackageName().equals("com.petcarehub.controller")) continue;
            for(String path:entry.getKey().getPatternValues()) for(var method:entry.getKey().getMethodsCondition().getMethods())
                actual.add(method.name()+" "+path.replaceFirst("^/api/v1", ""));
        }
        assertEquals(expected,actual,"Update the explicit endpoint policy inventory when adding/removing a route");
        for(String endpoint:actual) {
            int status=perform(endpoint,false);
            if(PUBLIC.contains(endpoint)) assertNotEquals(401,status,endpoint);
            else assertEquals(401,status,"Anonymous: "+endpoint);
        }
    }
    @Test void customerCannotReachAnyNonAllowlistedWrite() throws Exception {
        for(String endpoint:STAFF_WRITES) assertEquals(403,perform(endpoint,true),"Customer: "+endpoint);
    }
    private int perform(String endpoint, boolean customer) throws Exception {
        String[] parts=endpoint.split(" ",2);
        String path="/api/v1"+parts[1].replaceAll("\\{[^}]+}","987654321");
        var req=request(HttpMethod.valueOf(parts[0]),path).contentType("application/json").content("{}")
                .param("points","1").param("description","qa_test").param("role","CUSTOMER")
                .param("productId","987654321").param("quantity","1").param("reason","qa_test")
                .param("targetType","PRODUCT").param("targetId","987654321");
        if(customer) req.with(user(new UserDetailsImpl(987654321L,"qa_matrix","qa_matrix@example.test","unused",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))));
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }
}
