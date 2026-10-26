package com.petcarehub;

import com.petcarehub.entity.LoyaltyAccount;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.LoyaltyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser(roles="ADMIN")
class LoyaltyLimitsTest {
    @Autowired MockMvc mvc;
    @Autowired LoyaltyService loyalty;
    @MockBean LoyaltyAccountRepository accounts;
    @MockBean LoyaltyTransactionRepository transactions;
    @MockBean UserRepository users;
    UserDetailsImpl caller(String role) { return new UserDetailsImpl(1L,"caller","caller@example.test","unused",List.of(new SimpleGrantedAuthority("ROLE_"+role))); }

    @ParameterizedTest @ValueSource(ints={-1,0,1000001,Integer.MAX_VALUE})
    void invalidPointsReturn400WithoutMutation(int points) throws Exception {
        mvc.perform(post("/api/v1/loyalty/account/1/add").with(user(caller("ADMIN")))
                .param("points",String.valueOf(points)).param("description","test")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/loyalty/account/spend").with(user(caller("ADMIN")))
                .param("points",String.valueOf(points)).param("description","test")).andExpect(status().isBadRequest());
        verifyNoInteractions(accounts, transactions);
    }
    @Test void nullRejectedAtServiceBoundary() {
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> loyalty.addPoints(1L,null,"test"));
        assertThrows(com.petcarehub.exception.BadRequestException.class, () -> loyalty.spendPoints(1L,null,"test"));
        verifyNoInteractions(accounts, transactions);
    }
    @Test void accumulatedIntegerOverflowReturns400() throws Exception {
        when(users.findLockedById(1L)).thenReturn(Optional.of(new com.petcarehub.entity.User()));
        var account = LoyaltyAccount.builder().availablePoints(10).totalPoints(Integer.MAX_VALUE).build();
        when(accounts.findByCustomerId(1L)).thenReturn(Optional.of(account));
        mvc.perform(post("/api/v1/loyalty/account/1/add").with(user(caller("ADMIN")))
                .param("points","1").param("description","test")).andExpect(status().isBadRequest());
        verify(accounts, never()).save(any()); verifyNoInteractions(transactions);
    }
}
