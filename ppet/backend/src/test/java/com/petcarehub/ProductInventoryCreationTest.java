package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.dto.product.ProductCreateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.TransactionType;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.InventoryService;
import com.petcarehub.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:product-creation;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductInventoryCreationTest {
    @Autowired ProductService service;
    @Autowired InventoryService stock;
    @Autowired InventoryRepository inventories;
    @Autowired ProductCategoryRepository categories;
    @Autowired UserRepository users;
    @SpyBean InventoryTransactionRepository transactions;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    User actor;
    ProductCreateRequest request;

    @BeforeEach void setup() {
        String name = UUID.randomUUID().toString();
        actor = users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused")
                .firstName("Test").lastName("Creation").build());
        var principal = UserDetailsImpl.build(actor);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        var category = categories.save(ProductCategory.builder().name("Creation").build());
        request = new ProductCreateRequest(); request.setName("New product"); request.setCategoryId(category.getId());
        request.setPrice(BigDecimal.TEN); request.setSku(UUID.randomUUID().toString());
    }

    @AfterEach void clearPrincipal() { SecurityContextHolder.clearContext(); }

    @ParameterizedTest @NullSource @ValueSource(ints={0, 5})
    void createsExactlyOneInventoryThenAllowsFurtherStockReceipts(Integer initial) {
        request.setStockQuantity(initial);
        var result = service.createProduct(request);
        int expected = initial == null ? 0 : initial;
        assertEquals(expected, result.getStockQuantity());
        var inventory = inventories.findByProductId(result.getId()).orElseThrow();
        assertEquals(expected, inventory.getQuantity());
        assertEquals(1, jdbc.queryForObject("select count(*) from inventory where product_id=?", Integer.class, result.getId()));
        var ledger = transactions.findByInventoryId(inventory.getId(), Pageable.unpaged());
        assertEquals(expected > 0 ? 1 : 0, ledger.getTotalElements());
        if (expected > 0) {
            assertEquals(TransactionType.STOCK_IN, ledger.getContent().get(0).getTransactionType());
            assertEquals(expected, ledger.getContent().get(0).getQuantity());
        }
        var receipt = new StockUpdateRequest(); receipt.setProductId(result.getId()); receipt.setQuantity(3); receipt.setReason("Next receipt");
        stock.addStock(receipt);
        assertEquals(expected + 3, service.getProductById(result.getId()).getStockQuantity());
        assertEquals(inventory.getId(), inventories.findByProductId(result.getId()).orElseThrow().getId());
        assertEquals(expected > 0 ? 2 : 1, transactions.findByInventoryId(inventory.getId(), Pageable.unpaged()).getTotalElements());
    }

    @Test void failedOpeningStockLedgerRollsBackProductAndInventoryTogether() {
        request.setStockQuantity(5);
        long inventoryBefore = inventories.count(), ledgerBefore = transactions.count();
        doThrow(new DataIntegrityViolationException("Simulated ledger failure"))
                .when(transactions).save(any(InventoryTransaction.class));
        assertThrows(DataIntegrityViolationException.class, () -> service.createProduct(request));
        assertEquals(0, jdbc.queryForObject("select count(*) from products where sku=?", Integer.class, request.getSku()));
        assertEquals(inventoryBefore, inventories.count()); assertEquals(ledgerBefore, transactions.count());
    }

    @Test void customerCannotCreateAndNegativeInitialStockRemains400() throws Exception {
        request.setStockQuantity(0);
        mvc.perform(post("/api/v1/products").with(user(principal("CUSTOMER")))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        request.setStockQuantity(-1);
        mvc.perform(post("/api/v1/products").with(user(principal("SALES_STAFF")))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("select count(*) from products where sku=?", Integer.class, request.getSku()));
    }

    private UserDetailsImpl principal(String role) {
        return new UserDetailsImpl(actor.getId(), actor.getUsername(), actor.getEmail(), "unused",
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
