package com.petcarehub;

import com.petcarehub.dto.loyalty.LoyaltyAccountDTO;
import com.petcarehub.entity.*;
import com.petcarehub.repository.LoyaltyAccountRepository;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.LoyaltyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LoyaltyOwnershipTest {
    @Autowired MockMvc mvc;
    @MockBean LoyaltyService loyalty;
    @MockBean LoyaltyAccountRepository accounts;
    UserDetailsImpl principal(String role) {
        return new UserDetailsImpl(1L, "caller", "caller@example.test", "unused", List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
    @Test void selfEndpointsUsePrincipalNotQueryUserId() throws Exception {
        var account = new LoyaltyAccountDTO(); account.setId(10L);
        when(loyalty.getAccountByUserId(1L)).thenReturn(account);
        mvc.perform(get("/api/v1/loyalty/account?userId=2").with(user(principal("CUSTOMER")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/loyalty/transactions?accountId=20").with(user(principal("CUSTOMER")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/loyalty/account/spend?userId=2&points=1&description=test").with(user(principal("CUSTOMER")))).andExpect(status().isForbidden());
        verify(loyalty, never()).getAccountByUserId(2L);
        verify(loyalty).getTransactions(eq(10L), any());
        verify(loyalty, never()).spendPoints(any(), any(), any());
    }
    @Test void customerCannotReadSpendOrAddForAnotherUser() throws Exception {
        mvc.perform(get("/api/v1/loyalty/account/2").with(user(principal("CUSTOMER")))).andExpect(status().isForbidden());
        for (String action : List.of("add", "spend"))
            mvc.perform(post("/api/v1/loyalty/account/2/" + action + "?points=1&description=test").with(user(principal("CUSTOMER")))).andExpect(status().isForbidden());
        verifyNoInteractions(loyalty);
    }
    @Test void historyChecksAccountOwnerAndAllowsAdmin() throws Exception {
        var owner = new User(); owner.setId(2L);
        when(accounts.findById(20L)).thenReturn(Optional.of(LoyaltyAccount.builder().customer(owner).build()));
        mvc.perform(get("/api/v1/loyalty/account/20/transactions").with(user(principal("CUSTOMER")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/loyalty/account/20/transactions").with(user(principal("ADMIN")))).andExpect(status().isOk());
        verify(loyalty, times(1)).getTransactions(eq(20L), any());
    }
    @Test void adminCanAccessOtherUserAndAnonymousIsRejected() throws Exception {
        mvc.perform(get("/api/v1/loyalty/account/2").with(user(principal("ADMIN")))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/loyalty/account")).andExpect(status().isUnauthorized());
        verify(loyalty).getAccountByUserId(2L);
    }
}
