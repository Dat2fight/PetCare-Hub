package com.petcarehub;

import com.petcarehub.enums.RoleType;
import com.petcarehub.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CatalogAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean ProductService products;
    @MockBean ProductCategoryService categories;
    @MockBean PromotionService promotions;

    @ParameterizedTest @EnumSource(RoleType.class)
    void writeMatrixForAllThreeCatalogs(RoleType role) throws Exception {
        boolean allowed = role == RoleType.ADMIN || role == RoleType.SALES_STAFF;
        for (String catalog : new String[]{"products", "categories", "promotions"}) {
            String path = "/api/v1/" + catalog;
            String promotion = "{\"code\":\"TEST\",\"discountType\":\"PERCENTAGE\",\"discountValue\":10}";
            mvc.perform(post(path).with(user("caller").roles(role.name())).contentType("application/json")
                    .content(catalog.equals("products") ? "{\"name\":\"Product\",\"price\":10,\"categoryId\":1}" : catalog.equals("promotions") ? promotion : "{}"))
                    .andExpect(status().is(allowed ? (catalog.equals("promotions") ? 200 : 201) : 403));
            mvc.perform(put(path + "/1").with(user("caller").roles(role.name())).contentType("application/json").content(catalog.equals("promotions") ? promotion : "{}"))
                    .andExpect(status().is(allowed ? 200 : 403));
            mvc.perform(delete(path + "/1").with(user("caller").roles(role.name())))
                    .andExpect(status().is(allowed ? 204 : 403));
        }
    }
    @Test void anonymousCanReadButCannotWriteCatalogs() throws Exception {
        for (String catalog : new String[]{"products", "categories", "promotions"}) {
            mvc.perform(get("/api/v1/" + catalog)).andExpect(status().isOk());
            mvc.perform(delete("/api/v1/" + catalog + "/1")).andExpect(status().isUnauthorized());
            mvc.perform(post("/api/v1/" + catalog).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
            mvc.perform(put("/api/v1/" + catalog + "/1").contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }
    @Test void unauthorizedMalformedWritesAreRejectedBeforeDeserialization() throws Exception {
        for (String catalog : new String[]{"products", "categories", "promotions"}) {
            mvc.perform(post("/api/v1/" + catalog).with(user("qa_customer").roles("CUSTOMER"))
                    .contentType("application/json").content("{")) .andExpect(status().isForbidden());
            mvc.perform(put("/api/v1/" + catalog + "/1").with(user("qa_vet").roles("VETERINARIAN"))
                    .contentType("application/json").content("{")) .andExpect(status().isForbidden());
        }
    }
}
