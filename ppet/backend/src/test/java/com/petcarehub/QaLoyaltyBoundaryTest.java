package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.LoyaltyService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class QaLoyaltyBoundaryTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired LoyaltyAccountRepository accounts;
    @Autowired LoyaltyTransactionRepository ledger; @Autowired LoyaltyService loyalty;
    User a,b; LoyaltyAccount balance;
    User account() { String n="qa_"+UUID.randomUUID(); return users.save(User.builder().username(n).email(n+"@example.test")
            .passwordHash("unused").firstName("qa_A").lastName("qa_B").build()); }
    UserDetailsImpl caller(User u,String role) {return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
    @BeforeEach void seed() {
        a=account();b=account();
        balance=accounts.save(LoyaltyAccount.builder().customer(b).availablePoints(100).totalPoints(100).build());
    }
    @Test void customerCannotChangeOwnOrOtherBalanceThroughAnyHttpAlias() throws Exception {
        for(String path:List.of("/account/spend","/account/"+a.getId()+"/spend","/account/"+b.getId()+"/spend","/account/"+b.getId()+"/add"))
            mvc.perform(post("/api/v1/loyalty"+path).with(user(caller(a,"CUSTOMER"))).param("points","1").param("description","qa_test"))
                    .andExpect(status().isForbidden());
        assertEquals(100,accounts.findById(balance.getId()).orElseThrow().getAvailablePoints());
    }
    @Test @WithMockUser(roles="CUSTOMER") void serviceMutationCannotBypassAdminBoundary() {
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->loyalty.addPoints(b.getId(),1,"qa_test"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,()->loyalty.spendPoints(b.getId(),1,"qa_test"));
    }
    @Test void invalidOrInsufficientAdminSpendDoesNotChangeBalanceOrLedger() throws Exception {
        long before=ledger.count();
        for(int points:new int[]{0,-100,1000001,Integer.MAX_VALUE,101})
            mvc.perform(post("/api/v1/loyalty/account/{id}/spend",b.getId()).with(user(caller(a,"ADMIN")))
                    .param("points",String.valueOf(points)).param("description","qa_test")).andExpect(status().isBadRequest());
        assertEquals(100,accounts.findById(balance.getId()).orElseThrow().getAvailablePoints());
        assertEquals(before,ledger.count());
    }
    @Test void historyResourceHidesAnotherCustomersAccount() throws Exception {
        String path="/api/v1/loyalty/account/"+balance.getId()+"/transactions";
        mvc.perform(get(path).with(user(caller(a,"CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(get(path).with(user(caller(b,"CUSTOMER")))).andExpect(status().isOk());
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }
}
