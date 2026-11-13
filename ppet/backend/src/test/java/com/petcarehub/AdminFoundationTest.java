package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.*;
import com.petcarehub.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminFoundationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @Autowired AdminAuditRepository audit;
    @Autowired JwtTokenProvider tokens;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping mappings;
    @Autowired jakarta.persistence.EntityManager em;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired com.petcarehub.service.AdminAccountService accounts;

    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentAdministratorsCannotDisableEachOther() throws Exception {
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);
        String suffix=java.util.UUID.randomUUID().toString().substring(0,8);
        var actors=tx.execute(s -> java.util.List.of(account("race_a_"+suffix,RoleType.ADMIN),account("race_b_"+suffix,RoleType.ADMIN)));
        var ready=new java.util.concurrent.CountDownLatch(2); var start=new java.util.concurrent.CountDownLatch(1);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var results=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for(int i=0;i<2;i++) { final int index=i; results.add(executor.submit(() -> {
                var principal=UserDetailsImpl.build(actors.get(index));
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
                ready.countDown(); start.await();
                try { accounts.status(actors.get(1-index).getId(),new com.petcarehub.dto.admin.AdminRequests.Status(false,"concurrency regression","Test-password-123")); return true; }
                catch(org.springframework.security.access.AccessDeniedException denied) { return false; }
                finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
            })); }
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)); start.countDown();
            int successes=0; for(var result:results) if(result.get(20,java.util.concurrent.TimeUnit.SECONDS)) successes++;
            org.junit.jupiter.api.Assertions.assertEquals(1,successes);
            long active=tx.execute(s -> actors.stream().filter(a -> users.findById(a.getId()).orElseThrow().getEnabled()).count());
            org.junit.jupiter.api.Assertions.assertEquals(1,active);
        } finally { executor.shutdownNow(); tx.executeWithoutResult(s -> actors.forEach(a -> users.deleteById(a.getId()))); }
    }
    String token(User user) {
        var p=UserDetailsImpl.build(user);
        return tokens.generateToken(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));
    }
    org.springframework.test.web.servlet.request.RequestPostProcessor as(User u) { return user(UserDetailsImpl.build(u)); }
    User account(String name, RoleType role) {
        Role r=roles.findByName(role).orElseGet(() -> roles.save(Role.builder().name(role).build()));
        return users.saveAndFlush(User.builder().username(name).email(name+"@example.test")
            .firstName("Test").lastName("Admin").passwordHash(passwords.encode("Test-password-123"))
            .roles(Set.of(r)).build());
    }
    @Test void staffPermissionsComeFromBackendAndExcludeSensitiveAdministration() throws Exception {
        var staff=account("foundation_sales",RoleType.SALES_STAFF);
        mvc.perform(get("/api/v1/admin/me/permissions").with(user(UserDetailsImpl.build(staff))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.permissions",org.hamcrest.Matchers.hasItem("INVENTORY_MANAGE")))
            .andExpect(jsonPath("$.permissions",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("USER_MANAGE"))));
    }
    @Test void cannotLockSelfAndCanLockCustomer() throws Exception {
        var admin=account("foundation_admin",RoleType.ADMIN);
        var customer=account("foundation_customer",RoleType.CUSTOMER);
        mvc.perform(patch("/api/v1/admin/users/"+admin.getId()+"/status").with(user(UserDetailsImpl.build(admin)))
            .contentType("application/json").content("{\"enabled\":false,\"reason\":\"test\"}"))
            .andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/users/"+customer.getId()+"/status").with(user(UserDetailsImpl.build(admin)))
            .contentType("application/json").content("{\"enabled\":false,\"reason\":\"test\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }

    @Test void everyAdminEndpointHasExplicitRolePolicyAndMethodGuard() throws Exception {
        var expected=java.util.Set.of("GET /admin/me/permissions","GET /admin/users","POST /admin/staff",
            "GET /admin/dashboard","GET /admin/revenue-report","GET /admin/audit-logs","GET /admin/users/{userId}/overview",
            "PATCH /admin/users/{userId}/role","PATCH /admin/users/{userId}/status","DELETE /admin/users/{userId}","POST /admin/users/{userId}/revoke-sessions");
        var actual=new java.util.HashSet<String>();
        mappings.getHandlerMethods().forEach((mapping,handler) -> mapping.getPatternValues().stream().filter(p -> p.startsWith("/api/v1/admin/") && handler.getBeanType()==com.petcarehub.controller.AdminController.class).forEach(p -> {
            org.junit.jupiter.api.Assertions.assertNotNull(handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class),p);
            mapping.getMethodsCondition().getMethods().forEach(method -> actual.add(method+" "+p.substring(7)));
        }));
        org.junit.jupiter.api.Assertions.assertEquals(expected,actual);
        var target=account("matrix_target",RoleType.CUSTOMER);
        for(var role:RoleType.values()) {
            var actor=account("matrix_"+role,role);
            for(String endpoint:expected) {
                String[] parts=endpoint.split(" ",2); String path="/api/v1"+parts[1].replace("{userId}",target.getId().toString());
                boolean allowed=role==RoleType.ADMIN || (parts[1].equals("/admin/me/permissions") && role!=RoleType.CUSTOMER);
                int result=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(parts[0]),path).with(as(actor)).contentType("application/json").content("{"))
                    .andReturn().getResponse().getStatus();
                if(allowed) org.junit.jupiter.api.Assertions.assertTrue(java.util.Set.of(200,400).contains(result),endpoint+" "+role+" "+result);
                else org.junit.jupiter.api.Assertions.assertEquals(403,result,endpoint+" "+role);
                mvc.perform(request(org.springframework.http.HttpMethod.valueOf(parts[0]),path)).andExpect(status().isUnauthorized());
            }
        }
    }

    @Test void allAdminWritesCreateExactlyOneSuccessAuditAndOverviewIsAudited() throws Exception {
        var admin=account("coverage_admin",RoleType.ADMIN);
        roles.findByName(RoleType.SALES_STAFF).orElseGet(() -> roles.save(Role.builder().name(RoleType.SALES_STAFF).build()));
        var target=account("coverage_target",RoleType.CUSTOMER);
        var covered=new java.util.HashSet<String>();
        java.util.List<String[]> requests=java.util.List.of(
            new String[]{"POST","/admin/staff","{\"username\":\"coverage_staff\",\"email\":\"coverage_staff@example.test\",\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"SALES_STAFF\"}"},
            new String[]{"PATCH","/admin/users/{userId}/status","{\"enabled\":false,\"reason\":\"test\"}"},
            new String[]{"PATCH","/admin/users/{userId}/role","{\"role\":\"ADMIN\",\"reason\":\"test\",\"currentPassword\":\"Test-password-123\"}"},
            new String[]{"POST","/admin/users/{userId}/revoke-sessions","{\"reason\":\"test\"}"},
            new String[]{"DELETE","/admin/users/{userId}","{\"reason\":\"test\",\"currentPassword\":\"Test-password-123\"}"});
        for(var r:requests) {
            long before=audit.count();
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(r[0]),"/api/v1"+r[1].replace("{userId}",target.getId().toString()))
                .with(as(admin)).contentType("application/json").content(r[2])).andExpect(status().is2xxSuccessful());
            org.junit.jupiter.api.Assertions.assertEquals(before+1,audit.count(),r[1]); covered.add(r[0]+" "+r[1]);
        }
        var mapped=new java.util.HashSet<String>();
        mappings.getHandlerMethods().forEach((mapping,handler) -> mapping.getPatternValues().stream().filter(p -> p.startsWith("/api/v1/admin/") && handler.getBeanType()==com.petcarehub.controller.AdminController.class).forEach(p ->
            mapping.getMethodsCondition().getMethods().stream().filter(m -> !m.name().equals("GET")).forEach(m -> mapped.add(m.name()+" "+p.substring(7)))));
        org.junit.jupiter.api.Assertions.assertEquals(mapped,covered,"Every new admin write needs a successful audit assertion");
        long before=audit.count();
        mvc.perform(get("/api/v1/admin/users/"+target.getId()+"/overview").with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        org.junit.jupiter.api.Assertions.assertEquals(before+1,audit.count());
        var result=mvc.perform(get("/api/v1/admin/audit-logs").with(as(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertFalse(result.contains("Test-password-123"));
        org.junit.jupiter.api.Assertions.assertFalse(result.contains("passwordHash"));
        org.junit.jupiter.api.Assertions.assertFalse(result.contains("temporaryPassword"));
    }

    @Test void revokedTokensStayInvalidAfterUnlockAndRoleChanges() throws Exception {
        var admin=account("revoke_admin",RoleType.ADMIN); var target=account("revoke_customer",RoleType.CUSTOMER);
        String original=token(target);
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+original)).andExpect(status().isOk());
        for(boolean enabled:new boolean[]{false,true}) mvc.perform(patch("/api/v1/admin/users/"+target.getId()+"/status").with(as(admin)).contentType("application/json")
            .content("{\"enabled\":"+enabled+",\"reason\":\"test\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+original)).andExpect(status().isUnauthorized());
        String fresh=token(target);
        mvc.perform(post("/api/v1/admin/users/"+target.getId()+"/revoke-sessions").with(as(admin)).contentType("application/json").content("{\"reason\":\"test\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+fresh)).andExpect(status().isUnauthorized());
    }

    @Test void temporaryPasswordOnlyAllowsPasswordChangeAndIsNeverReturnedAgain() throws Exception {
        var admin=account("temp_admin",RoleType.ADMIN);
        roles.findByName(RoleType.VETERINARIAN).orElseGet(() -> roles.save(Role.builder().name(RoleType.VETERINARIAN).build()));
        String body=mvc.perform(post("/api/v1/admin/staff").with(as(admin)).contentType("application/json")
            .content("{\"username\":\"temp_vet\",\"email\":\"temp_vet@example.test\",\"firstName\":\"Vet\",\"lastName\":\"Test\",\"role\":\"VETERINARIAN\",\"password\":\"client-chosen-password\"}"))
            .andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.user.mustChangePassword").value(true)).andReturn().getResponse().getContentAsString();
        String temporary=json.readTree(body).get("temporaryPassword").asText();
        var employee=users.findByUsername("temp_vet").orElseThrow(); String jwt=token(employee);
        mvc.perform(get("/api/v1/admin/me/permissions").header("Authorization","Bearer "+jwt)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+jwt)).andExpect(status().isOk()).andExpect(jsonPath("$.temporaryPassword").doesNotExist());
        mvc.perform(post("/api/v1/auth/password").header("Authorization","Bearer "+jwt).contentType("application/json")
            .content(json.writeValueAsString(java.util.Map.of("currentPassword",temporary,"newPassword","Updated-secret-123")))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+jwt)).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertFalse(employee.isMustChangePassword());
        org.junit.jupiter.api.Assertions.assertTrue(passwords.matches("Updated-secret-123",employee.getPasswordHash()));
        mvc.perform(get("/api/v1/admin/me/permissions").header("Authorization","Bearer "+token(employee))).andExpect(status().isOk());
    }

    @Test void badConfirmationLogsDeniedAndRollsBackRoleMutation() throws Exception {
        var admin=account("denied_admin",RoleType.ADMIN); var target=account("denied_target",RoleType.CUSTOMER);
        long before=audit.count();
        var response=mvc.perform(patch("/api/v1/admin/users/"+target.getId()+"/role").with(as(admin)).contentType("application/json")
            .content("{\"role\":\"ADMIN\",\"reason\":\"test\",\"currentPassword\":\"incorrect-secret\"}"))
            .andExpect(status().isForbidden()).andReturn().getResponse();
        org.junit.jupiter.api.Assertions.assertEquals(before+1,audit.count());
        org.junit.jupiter.api.Assertions.assertEquals(response.getHeader("X-Correlation-ID"),json.readTree(response.getContentAsString()).get("correlationId").asText());
        org.junit.jupiter.api.Assertions.assertTrue(target.getRoles().stream().allMatch(r -> r.getName()==RoleType.CUSTOMER));
        mvc.perform(patch("/api/v1/admin/users/"+target.getId()+"/role").with(as(admin)).contentType("application/json")
            .content("{\"role\":\"SALES_STAFF\",\"reason\":\"test\"}")).andExpect(status().isBadRequest());
    }

    @Test void searchIsBoundedLiteralAndDeletedAccountsCannotLogin() throws Exception {
        var admin=account("search_admin",RoleType.ADMIN); var target=account("search_target",RoleType.CUSTOMER);
        mvc.perform(get("/api/v1/admin/users?keyword=%25&size=10000").with(as(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/users?sort=passwordHash,asc").with(as(admin))).andExpect(status().isBadRequest());
        String jwt=token(target);
        mvc.perform(delete("/api/v1/admin/users/"+target.getId()).with(as(admin)).contentType("application/json").content("{\"reason\":\"test\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+jwt)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content("{\"username\":\"search_target\",\"password\":\"Test-password-123\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users?status=DELETED&keyword=search_target").with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
}
