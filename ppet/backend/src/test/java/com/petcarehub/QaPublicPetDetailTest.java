package com.petcarehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class QaPublicPetDetailTest {
    @Autowired MockMvc mvc;
    @Test void missingPetReturnsVietnamese404NotSampleData() throws Exception {
        mvc.perform(get("/api/v1/pets/"+Long.MAX_VALUE)).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Không tìm thấy thú cưng"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty()).andExpect(jsonPath("$.price").doesNotExist());
    }
}
