package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:admin-users;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminUsersPaginationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired RoleRepository roles;
    @SpyBean UserRepository users;
    List<Long> ids;

    @BeforeEach void setup() {
        Role role=roles.save(Role.builder().name(RoleType.CUSTOMER).build());
        String prefix=UUID.randomUUID().toString(); ids=new ArrayList<>();
        for(int i=0;i<105;i++) {
            String name=prefix+String.format("%03d",i);
            ids.add(users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused-test-hash")
                    .firstName("Test").lastName("Account").phone("0000000000").avatarUrl("/private-avatar")
                    .enabled(i%2==0).roles(Set.of(role)).build()).getId());
        }
        users.flush(); clearInvocations(users);
    }

    @Test void defaultPageUsesBoundedRepositoryQueryAndExplicitFields() throws Exception {
        var result=mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.totalElements").value(105)).andExpect(jsonPath("$.totalPages").value(6))
                .andExpect(jsonPath("$.content[0].id").value(ids.get(0))).andExpect(jsonPath("$.content[0].roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.content[0].enabled").value(true)).andReturn();
        var fields=new java.util.HashSet<String>();json.readTree(result.getResponse().getContentAsString()).get("content").get(0).fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("id","username","email","firstName","lastName","enabled","roles"),fields);
        verify(users).findAll(argThat((Pageable page)->page.getPageNumber()==0 && page.getPageSize()==20 && page.getSort().getOrderFor("id")!=null));
        verify(users,never()).findAll();
    }

    @Test void pageBoundariesMaximumAndStableSortAreApplied() throws Exception {
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("page","1").param("size","7"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(7))
                .andExpect(jsonPath("$.content[0].id").value(ids.get(7))).andExpect(jsonPath("$.content[6].id").value(ids.get(13)));
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("size","10000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.content.length()").value(100));
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("page","1").param("size","100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(5)).andExpect(jsonPath("$.last").value(true));
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("page","99").param("size","100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0)).andExpect(jsonPath("$.totalElements").value(105));
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("size","2").param("sort","username,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(ids.get(104)));
        verify(users,never()).findAll();
    }

    @Test void permissionsAndSortValidationRemainEnforced() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        for(String role:List.of("CUSTOMER","SALES_STAFF","WAREHOUSE_STAFF","VETERINARIAN"))
            mvc.perform(get("/api/v1/admin/users").with(user("other").roles(role))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").with(user("admin").roles("ADMIN")).param("sort","passwordHash,asc"))
                .andExpect(status().isBadRequest());
        verify(users,never()).findAll(any(Pageable.class)); verify(users,never()).findAll();
    }
}
