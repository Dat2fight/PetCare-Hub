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
        "GET /home/products", "GET /home/reviews", "GET /services", "GET /services/{id}", "GET /species", "GET /species/{speciesId}/breeds",
        "GET /promotions", "GET /promotions/{id}", "GET /promotions/validate/{code}",
        "GET /care-packages","GET /care-packages/{id}", "GET /reviews/summary", "GET /reviews", "GET /reviews/{id}");
    // Customer operations must obtain identity from the principal and enforce resource ownership.
    static final Set<String> CUSTOMER_WRITES = Set.of(
        "POST /auth/password",
        "POST /cart/items", "PUT /cart/items/{itemId}", "DELETE /cart/items/{itemId}", "DELETE /cart",
        "POST /orders/quote", "POST /orders", "POST /orders/{id}/cancel", "POST /appointments", "POST /appointments/{id}/cancel", "PUT /appointments/{id}/status",
        "PUT /reviews/me/{id}", "DELETE /reviews/me/{id}", "POST /wishlist", "DELETE /wishlist/{id}", "POST /reviews", "PUT /users/me", "POST /pets/my-pets", "PUT /pets/my-pets/{id}",
        "PUT /notifications/{id}/read", "PUT /notifications/read-all",
        "POST /pet-matching/submit", "POST /pet-matching/submit/{userId}");
    static final Set<String> STAFF_WRITES = Set.of(
        "POST /admin/notifications/send","POST /admin/notifications/templates","PUT /admin/notifications/templates/{id}","DELETE /admin/notifications/templates/{id}",
        "POST /admin/catalog/{resource}","PUT /admin/catalog/{resource}/{id}","DELETE /admin/catalog/{resource}/{id}","POST /admin/catalog/products/bulk-preview","POST /admin/catalog/products/bulk-apply","POST /admin/catalog/products/csv-preview","POST /admin/catalog/products/csv-apply","POST /admin/catalog/pets/{id}/release-reservation","POST /admin/schedules/{kind}","PUT /admin/schedules/{kind}/{id}","DELETE /admin/schedules/{kind}/{id}","POST /admin/schedule-cancel","PUT /admin/loyalty-rules","POST /admin/loyalty/{userId}/adjust","POST /admin/subscriptions/{id}","POST /admin/campaigns",
        "POST /admin/orders/{id}/transition","POST /admin/orders/{id}/request-refund","POST /admin/orders/{id}/mark-refunded","POST /admin/orders/{id}/notes","POST /admin/inventory/movements","POST /admin/appointments/{id}/assign","POST /admin/appointments/{id}/status","POST /admin/reviews/{id}/moderate",
        "PATCH /admin/users/{userId}/status", "DELETE /admin/users/{userId}", "POST /admin/users/{userId}/revoke-sessions",
        "POST /products", "PUT /products/{id}", "DELETE /products/{id}",
        "POST /categories", "PUT /categories/{id}", "DELETE /categories/{id}",
        "POST /promotions", "PUT /promotions/{id}", "DELETE /promotions/{id}",
        "POST /services", "PUT /services/{id}", "DELETE /services/{id}",
        "POST /pets", "PUT /pets/{id}", "DELETE /pets/{id}",
        "POST /medical-records", "PUT /medical-records/{id}", "DELETE /medical-records/{id}",
        "POST /vaccinations", "PUT /vaccinations/{id}", "DELETE /vaccinations/{id}",
        "POST /treatments", "PUT /treatments/{id}", "DELETE /treatments/{id}",
        "POST /inventory/add", "POST /inventory/reduce", "POST /inventory/adjust",
        "POST /care-packages","PUT /care-packages/{id}","DELETE /care-packages/{id}","PUT /reviews/{id}/reject", "PUT /reviews/{id}/hide", "PUT /reviews/{id}/approve", "POST /admin/staff", "PATCH /admin/users/{userId}/role",
        "POST /orders/{id}/confirm", "POST /orders/{id}/process", "POST /orders/{id}/ship", "POST /orders/{id}/deliver",
        "POST /loyalty/account/{userId}/add", "POST /loyalty/account/spend", "POST /loyalty/account/{userId}/spend");
    static final Set<String> PRIVATE_READS = Set.of(
        "GET /admin/dashboard/summary","GET /admin/dashboard/alerts","GET /admin/reports/revenue","GET /admin/reports/rankings","GET /admin/reports/export","GET /admin/system/status","GET /admin/notifications/audience-preview","GET /admin/notifications/history","GET /admin/notifications/templates",
        "GET /admin/catalog/products/low-stock","GET /admin/catalog/{resource}","GET /admin/catalog/{resource}/{id}","GET /admin/pet-support/{id}","GET /admin/schedules/{kind}","GET /admin/schedule-affected","GET /admin/staff-performance","GET /admin/loyalty-rules","GET /admin/promotion-usages/{id}","GET /admin/subscriptions","GET /admin/campaign-audience",
        "GET /admin/orders","GET /admin/orders/{id}","GET /admin/orders/{id}/print","GET /admin/orders/{id}/notes","GET /admin/orders/{id}/payments","GET /admin/inventory","GET /admin/inventory/transactions","GET /admin/appointments","GET /admin/appointments/{id}","GET /admin/appointments/{id}/eligible-staff","GET /admin/reviews","GET /admin/reviews/count",
        "GET /admin/me/permissions", "GET /admin/users/{userId}/overview", "GET /admin/audit-logs",
        "GET /care-packages/manage","GET /care-packages/subscriptions/me","GET /wishlist", "GET /wishlist/status", "GET /reviews/me", "GET /reviews/pending", "GET /reviews/eligibility", "GET /auth/me", "GET /cart", "GET /orders", "GET /orders/{id}",
        "GET /pets/my-pets", "GET /pets/my-pets/{id}", "GET /products/manage", "GET /services/manage",
        "GET /appointments", "GET /appointments/{id}", "GET /appointments/me", "GET /appointments/user/{userId}", "GET /appointments/pet/{petId}", "GET /appointments/slots",
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
    @Test void everyStaffWriteChecksEveryRealRoleBeforeBinding() throws Exception {
        for(var role:com.petcarehub.enums.RoleType.values()) for(String endpoint:STAFF_WRITES) {
            boolean allowed=role==com.petcarehub.enums.RoleType.ADMIN;
            if(endpoint.matches(".* /(care-packages|products|categories|promotions|services|pets|orders|reviews)(/.*)?"))
                allowed |= role==com.petcarehub.enums.RoleType.SALES_STAFF;
            if(endpoint.matches(".* /(medical-records|vaccinations|treatments)(/.*)?"))
                allowed |= role==com.petcarehub.enums.RoleType.VETERINARIAN;
            if(endpoint.contains(" /inventory/")) allowed |= role==com.petcarehub.enums.RoleType.WAREHOUSE_STAFF;
            if(endpoint.startsWith("POST /admin/orders/")||endpoint.startsWith("POST /admin/reviews/")) allowed |= role==com.petcarehub.enums.RoleType.SALES_STAFF;
            if(endpoint.startsWith("POST /admin/inventory/")) allowed |= role==com.petcarehub.enums.RoleType.SALES_STAFF || role==com.petcarehub.enums.RoleType.WAREHOUSE_STAFF;
            if(endpoint.equals("POST /admin/appointments/{id}/status")) allowed |= role==com.petcarehub.enums.RoleType.SALES_STAFF||role==com.petcarehub.enums.RoleType.VETERINARIAN;
            if(endpoint.startsWith("POST /admin/catalog/")||endpoint.startsWith("PUT /admin/catalog/")||endpoint.startsWith("DELETE /admin/catalog/")||endpoint.equals("POST /admin/campaigns")||endpoint.startsWith("POST /admin/subscriptions/")) allowed |= role==com.petcarehub.enums.RoleType.SALES_STAFF;
            // Malformed JSON prevents test data mutations after an allowed filter boundary.
            // CatalogAuthorizationTest and flow tests exercise valid allowed requests as well.
            int status=perform(endpoint,role.name(),true);
            // The synthetic staff principal has no backing account for this manual stock movement.
            if (allowed && endpoint.equals("POST /inventory/reduce")) assertEquals(401, status);
            else if(allowed) assertTrue(Set.of(200,201,204,400,404,409).contains(status),role+": "+endpoint+" -> "+status);
            else assertEquals(403,status,role+": "+endpoint);
        }
    }
    @Test void everyPrivateResourceIdHasANamedOwnershipRegression() throws Exception {
        Map<String,String> evidence=new HashMap<>();
        register(evidence,"AdminCatalogTest#roleMatrixAndCompleteEndpointInventory","GET /admin/catalog/{resource}","GET /admin/catalog/{resource}/{id}","GET /admin/pet-support/{id}","GET /admin/schedules/{kind}","GET /admin/promotion-usages/{id}");
        register(evidence,"AdminOperationsTest#roleMatrixAndCompleteEndpointInventory","GET /admin/orders/{id}","GET /admin/orders/{id}/print","GET /admin/orders/{id}/notes","GET /admin/orders/{id}/payments","GET /admin/appointments/{id}","GET /admin/appointments/{id}/eligible-staff");
        register(evidence,"AdminFoundationTest#everyAdminEndpointHasExplicitRolePolicyAndMethodGuard", "GET /admin/users/{userId}/overview");
        register(evidence,"PrivatePetDataTest#privatePetEndpointRequiresOwnerOrClinicalRole", "GET /pets/my-pets/{id}");
        register(evidence,"PrivatePetDataTest#personalPetUpdatesCheckOwnershipAndIgnoreProtectedFields", "PUT /pets/my-pets/{id}");
        register(evidence,"QaAppointmentScopeTest#customerCannotDiscoverOrCancelAnotherCustomersAppointment",
                "GET /appointments/user/{userId}","GET /appointments/pet/{petId}","PUT /appointments/{id}/status",
                "GET /appointments/{id}","POST /appointments/{id}/cancel");
        register(evidence,"PrivatePetDataTest#clinicalDetailsAndHistoriesRejectOtherCustomersAndPreserveClinicalAccess",
                "GET /medical-records/{id}","GET /medical-records/pet/{petId}","GET /vaccinations/{id}","GET /vaccinations/pet/{petId}");
        register(evidence,"TreatmentFlowTest#veterinarianAndAdminCrudPersistsAllRequiredFields", "GET /treatments/{id}","GET /treatments/record/{recordId}");
        register(evidence,"PrivatePetDataTest#notificationsStayStrictlyPerPrincipalIncludingAdmin", "GET /notifications/{userId}","PUT /notifications/{id}/read");
        register(evidence,"OrderFlowTest#persistsServerPriceAndProtectedDetail", "GET /orders/{id}","POST /orders/{id}/cancel");
        register(evidence,"CartMutationTest#foreignItemCannotBeUpdatedOrRemovedAndQueryIdCannotSelectOwner", "PUT /cart/items/{itemId}","DELETE /cart/items/{itemId}");
        register(evidence,"LoyaltyOwnershipTest#customerCannotReadSpendOrAddForAnotherUser", "GET /loyalty/account/{userId}");
        register(evidence,"QaLoyaltyBoundaryTest#historyResourceHidesAnotherCustomersAccount", "GET /loyalty/account/{accountId}/transactions");
        register(evidence,"MatchingTransactionTest#legacyUrlCannotOverridePrincipalCustomer", "POST /pet-matching/submit/{userId}");
        register(evidence,"PhaseThreeOwnershipTest#foreignReview", "PUT /reviews/me/{id}", "DELETE /reviews/me/{id}");
        register(evidence,"PhaseThreeOwnershipTest#wishlistIsolation", "DELETE /wishlist/{id}");
        // Staff-only writes are covered by the role boundary matrix; public catalog IDs have no customer owner.
        Set<String> resourceIds=new TreeSet<>();
        for(var group:List.of(CUSTOMER_WRITES,PRIVATE_READS)) for(String endpoint:group)
            if(endpoint.contains("{") && !endpoint.startsWith("GET /inventory/")) resourceIds.add(endpoint);
        assertEquals(resourceIds,new TreeSet<>(evidence.keySet()),"Missing private resource ownership test; review before adding route");
        for(String reference:new HashSet<>(evidence.values())) {
            String[] parts=reference.split("#");
            assertTrue(Arrays.stream(Class.forName("com.petcarehub."+parts[0]).getDeclaredMethods()).anyMatch(m->m.getName().equals(parts[1])),reference);
        }
    }
    private static void register(Map<String,String> evidence,String test,String... endpoints) {
        for(String endpoint:endpoints) evidence.put(endpoint,test);
    }
    private int perform(String endpoint, boolean customer) throws Exception {
        return perform(endpoint,customer ? "CUSTOMER" : null,false);
    }
    private int perform(String endpoint, String role, boolean malformed) throws Exception {
        String[] parts=endpoint.split(" ",2);
        String path="/api/v1"+parts[1].replace("{resource}","products").replace("{kind}","hours").replaceAll("\\{[^}]+}","987654321");
        var req=request(HttpMethod.valueOf(parts[0]),path).contentType("application/json").content(malformed ? "{" : "{}")
                .param("points","1").param("description","qa_test").param("role","CUSTOMER")
                .param("productId","987654321").param("quantity","1").param("reason","qa_test")
                .param("targetType","PRODUCT").param("targetId","987654321");
        if(role!=null) req.with(user(new UserDetailsImpl(987654321L,"qa_matrix","qa_matrix@example.test","unused",
                List.of(new SimpleGrantedAuthority("ROLE_"+role)))));
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }
}
