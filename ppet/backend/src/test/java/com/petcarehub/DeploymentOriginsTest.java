package com.petcarehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="app.cors.allowed-origins=https://shop.example.test, https://staff.example.test")
@AutoConfigureMockMvc @ActiveProfiles("test")
class DeploymentOriginsTest {
    @Autowired MockMvc mvc;
    @Test void configuredFrontendOriginsCanPreflightApi() throws Exception {
        for(String origin:new String[]{"https://shop.example.test","https://staff.example.test"})
            mvc.perform(options("/api/v1/cart").header("Origin",origin)
                    .header("Access-Control-Request-Method","GET").header("Access-Control-Request-Headers","authorization"))
                    .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin));
    }
    @Test void otherOriginIsDeniedForApiAndSockJs() throws Exception {
        mvc.perform(options("/api/v1/cart").header("Origin","https://other.example.test")
                .header("Access-Control-Request-Method","GET")).andExpect(status().isForbidden());
        mvc.perform(get("/ws/info").header("Origin","https://other.example.test")).andExpect(status().isForbidden());
    }
    @Test void sockJsUsesTheSameConfiguredOrigins() throws Exception {
        mvc.perform(get("/ws/info").header("Origin","https://staff.example.test"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","https://staff.example.test"));
    }
}
