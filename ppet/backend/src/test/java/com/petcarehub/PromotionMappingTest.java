package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.promotion.PromotionCreateRequest;
import com.petcarehub.enums.DiscountType;
import com.petcarehub.repository.PromotionRepository;
import com.petcarehub.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:promotion-mapping;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class PromotionMappingTest {
    @Autowired PromotionService service; @Autowired PromotionRepository repository;
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    PromotionCreateRequest request() {
        var r=new PromotionCreateRequest();r.setCode(UUID.randomUUID().toString());
        r.setDiscountType(DiscountType.PERCENTAGE);r.setDiscountValue(BigDecimal.TEN);r.setMaxUses(10);
        r.setMinOrderAmount(BigDecimal.valueOf(50));r.setActive(false);return r;
    }
    @Test void persistsExplicitFieldsAndPreservesUsageOnUpdate() {
        var r=request();var dto=service.createPromotion(r);
        assertEquals(DiscountType.PERCENTAGE,dto.getDiscountType());assertEquals(10,dto.getMaxUses());assertFalse(dto.getActive());
        var stored=repository.findById(dto.getId()).orElseThrow();stored.setCurrentUses(2);repository.save(stored);
        r.setDiscountType(DiscountType.FIXED_AMOUNT);r.setDiscountValue(BigDecimal.valueOf(200));r.setActive(true);
        var updated=service.updatePromotion(dto.getId(),r);
        assertEquals(DiscountType.FIXED_AMOUNT,updated.getDiscountType());assertEquals(0,updated.getDiscountValue().compareTo(BigDecimal.valueOf(200)));
        assertEquals(2,updated.getCurrentUses());assertEquals(0,updated.getMinOrderAmount().compareTo(BigDecimal.valueOf(50)));
        assertTrue(service.validatePromotion(r.getCode()));
    }
    @Test void rejectsInvalidDiscountsLimitsAndDates() throws Exception {
        for (String amount:new String[]{"0","-1","100.01","1.001","100000000"}) {
            var r=request();r.setDiscountValue(new BigDecimal(amount));
            mvc.perform(post("/api/v1/promotions").with(user("admin").roles("ADMIN"))
                    .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
        }
        var r=request();r.setMaxUses(0);
        mvc.perform(post("/api/v1/promotions").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
        r.setMaxUses(5);r.setStartDate(LocalDateTime.now());r.setEndDate(r.getStartDate().minusDays(1));
        mvc.perform(post("/api/v1/promotions").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(json.writeValueAsString(r))).andExpect(status().isBadRequest());
    }
    @Test void customerCannotWriteButSalesCanUseCanonicalContract() throws Exception {
        var payload=json.writeValueAsString(request());
        mvc.perform(post("/api/v1/promotions").with(user("customer").roles("CUSTOMER"))
                .contentType("application/json").content(payload)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/promotions").with(user("sales").roles("SALES_STAFF"))
                .contentType("application/json").content(payload)).andExpect(status().isOk())
                .andExpect(jsonPath("$.discountType").value("PERCENTAGE")).andExpect(jsonPath("$.discountValue").value(10))
                .andExpect(jsonPath("$.active").value(false)).andExpect(jsonPath("$.maxUses").value(10));
    }
}
