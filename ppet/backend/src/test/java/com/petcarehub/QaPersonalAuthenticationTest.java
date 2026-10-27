package com.petcarehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class QaPersonalAuthenticationTest {
    @Autowired MockMvc mvc;
    @Test void everyPersonalReadRejectsAnonymousWithConsistentJson() throws Exception {
        for (String path : List.of("/auth/me", "/pets/my-pets", "/pets/my-pets/1", "/appointments/me",
                "/orders", "/cart", "/notifications/unread-count", "/loyalty/account", "/loyalty/transactions")) {
            mvc.perform(get("/api/v1" + path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Vui lòng đăng nhập"))
                    .andExpect(jsonPath("$.path").value("/api/v1" + path))
                    .andExpect(jsonPath("$.correlationId").isNotEmpty());
        }
    }
    @Test void invalidBearerAndForbiddenWriteUseSameErrorSchema() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer qa_invalid"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.correlationId").isNotEmpty());
        mvc.perform(post("/api/v1/products").with(user("qa_customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
