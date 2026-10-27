package com.petcarehub;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:balances;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
@org.springframework.security.test.context.support.WithMockUser(roles="ADMIN")
class BalanceAndCartConcurrencyTest {
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired ProductCategoryRepository categories;
    @Autowired InventoryRepository inventories;
    @Autowired InventoryTransactionRepository stockTransactions;
    @Autowired LoyaltyAccountRepository accounts;
    @Autowired LoyaltyTransactionRepository pointTransactions;
    @Autowired LoyaltyService loyalty;
    @Autowired CartService carts;
    @Autowired InventoryService stock;
    @Autowired JdbcTemplate jdbc;

    private User customer() {
        String name = UUID.randomUUID().toString();
        return users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused")
                .firstName("Test").lastName("Concurrency").build());
    }

    private Product product(int quantity) {
        var category = categories.save(ProductCategory.builder().name("Concurrency").build());
        var product = products.save(Product.builder().name("Concurrency").category(category).sku(UUID.randomUUID().toString())
                .price(BigDecimal.ONE).active(true).build());
        inventories.save(Inventory.builder().product(product).quantity(quantity).build());
        return product;
    }

    private AddToCartRequest item(Product product, int quantity) {
        var request = new AddToCartRequest(); request.setProductId(product.getId()); request.setQuantity(quantity); return request;
    }

    private StockUpdateRequest incoming(Product product, int quantity) {
        var request = new StockUpdateRequest(); request.setProductId(product.getId()); request.setQuantity(quantity); request.setReason("Concurrency test"); return request;
    }

    private int race(User user, Runnable first, Runnable second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        var principal = new UserDetailsImpl(user.getId(), user.getUsername(), user.getEmail(), "unused",
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        java.util.function.Function<Runnable, Callable<Boolean>> task = action -> () -> {
            ready.countDown(); assertTrue(start.await(10, TimeUnit.SECONDS));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            try { action.run(); return true; }
            catch (BadRequestException insufficient) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            var calls = List.of(pool.submit(task.apply(first)), pool.submit(task.apply(second)));
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            int successes = 0;
            for (var call : calls) if (call.get(20, TimeUnit.SECONDS)) successes++;
            return successes;
        } finally { start.countDown(); pool.shutdownNow(); }
    }

    @Test void twoSpendsOf80From100AllowOnlyOneAndOneLedgerEntry() throws Exception {
        User user = customer(); loyalty.addPoints(user.getId(), 100, "Opening balance");
        Runnable spend = () -> loyalty.spendPoints(user.getId(), 80, "Spend");
        assertEquals(1, race(user, spend, spend));
        var account = accounts.findByCustomerId(user.getId()).orElseThrow();
        assertEquals(20, account.getAvailablePoints()); assertEquals(100, account.getTotalPoints());
        assertEquals(2, pointTransactions.findByLoyaltyAccountId(account.getId(), Pageable.unpaged()).getTotalElements());
    }

    @Test void concurrentFirstEarningsCreateOneAccountAndLoseNoPoints() throws Exception {
        User user = customer(); Runnable earn = () -> loyalty.addPoints(user.getId(), 80, "Earn");
        assertEquals(2, race(user, earn, earn));
        var account = accounts.findByCustomerId(user.getId()).orElseThrow();
        assertEquals(160, account.getAvailablePoints()); assertEquals(160, account.getTotalPoints());
        assertEquals(1, jdbc.queryForObject("select count(*) from loyalty_accounts where customer_id=?", Integer.class, user.getId()));
        assertEquals(2, pointTransactions.findByLoyaltyAccountId(account.getId(), Pageable.unpaged()).getTotalElements());
    }

    @Test void firstAccountReadAndEarnShareTheCreationLock() throws Exception {
        User user = customer();
        assertEquals(2, race(user, () -> loyalty.getAccountByUserId(user.getId()), () -> loyalty.addPoints(user.getId(), 80, "Earn")));
        assertEquals(80, loyalty.getAccountByUserId(user.getId()).getCurrentPoints());
        assertEquals(1, jdbc.queryForObject("select count(*) from loyalty_accounts where customer_id=?", Integer.class, user.getId()));
    }

    @Test void concurrentFirstCartAddsProduceOneLineWithBothQuantities() throws Exception {
        User user = customer(); Product product = product(10);
        Runnable add = () -> carts.addItemToCart(user.getId(), item(product, 2));
        assertEquals(2, race(user, add, add));
        var cart = carts.getCurrentUserCart(user.getId());
        assertEquals(1, cart.getItems().size()); assertEquals(4, cart.getItems().get(0).getQuantity());
        assertEquals(1, jdbc.queryForObject("select count(*) from carts where customer_id=?", Integer.class, user.getId()));
    }

    @Test void concurrentAddsToExistingLineCannotExceedStock() throws Exception {
        User user = customer(); Product product = product(5);
        carts.addItemToCart(user.getId(), item(product, 4));
        Runnable add = () -> carts.addItemToCart(user.getId(), item(product, 1));
        assertEquals(1, race(user, add, add));
        var cart = carts.getCurrentUserCart(user.getId());
        assertEquals(1, cart.getItems().size()); assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    @Test void concurrentStockReceiptsPreserveBothQuantitiesAndAuditEntries() throws Exception {
        User user = customer(); Product product = product(1);
        Runnable add = () -> stock.addStock(incoming(product, 2));
        assertEquals(2, race(user, add, add));
        var inventory = inventories.findByProductId(product.getId()).orElseThrow();
        assertEquals(5, inventory.getQuantity());
        assertEquals(2, stockTransactions.findByInventoryId(inventory.getId(), Pageable.unpaged()).getTotalElements());
    }

    @Test void concurrentReceiptAndWithdrawalDoNotOverwriteEachOther() throws Exception {
        User user = customer(); Product product = product(100);
        assertEquals(2, race(user, () -> stock.addStock(incoming(product, 80)), () -> stock.reduceStock(product.getId(), 80)));
        var inventory = inventories.findByProductId(product.getId()).orElseThrow();
        assertEquals(100, inventory.getQuantity());
        assertEquals(2, stockTransactions.findByInventoryId(inventory.getId(), Pageable.unpaged()).getTotalElements());
    }
}
