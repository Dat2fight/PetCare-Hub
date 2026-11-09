package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.order.*;
import com.petcarehub.entity.*;
import com.petcarehub.entity.Order;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.OrderService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pet-purchase;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000")
@AutoConfigureMockMvc @ActiveProfiles("test")
class PetPurchaseTest {
    @Autowired PetRepository pets; @Autowired SpeciesRepository species; @Autowired UserRepository users;
    @Autowired ProductRepository products; @Autowired ProductCategoryRepository categories;
    @Autowired InventoryRepository inventory; @Autowired InventoryTransactionRepository transactions;
    @Autowired CartRepository carts;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Autowired OrderRepository orders; @Autowired OrderService service; @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    User customer; Pet pet;
    User account() { String n=UUID.randomUUID().toString(); return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build()); }
    UserDetailsImpl caller(User u, String role) { return new UserDetailsImpl(u.getId(),u.getUsername(),u.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role))); }
    void authenticate(User u,String role) { var p=caller(u,role); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities())); }
    Pet sale() { return pets.save(Pet.builder().name("Sale pet").species(species.save(Species.builder().name(UUID.randomUUID().toString()).build())).price(new BigDecimal("1500.00")).imageUrl("/pet.png").build()); }
    OrderItemDTO item(Long id) { var i=new OrderItemDTO(); i.setPetId(id); i.setQuantity(1); return i; }
    OrderCreateRequest request(OrderItemDTO... items) { var r=new OrderCreateRequest();r.setItems(List.of(items));r.setShippingAddress("Địa chỉ test");return r; }
    // Persist a legacy order fixture directly: the public creation flow must remain closed.
    Order existingOrder(Product product, Pet... reserved) {
        return new org.springframework.transaction.support.TransactionTemplate(transactionManager).execute(status -> {
            Order order=Order.builder().customer(customer).orderNumber("LEGACY-"+UUID.randomUUID())
                    .paymentMethod("COD").shippingAddress("Địa chỉ test").build();
            BigDecimal total=BigDecimal.ZERO;
            for(Pet animal:reserved) {
                order.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(animal).quantity(1)
                        .unitPrice(animal.getPrice()).subtotal(animal.getPrice()).build());
                total=total.add(animal.getPrice());
            }
            if(product!=null) {
                BigDecimal subtotal=product.getPrice().multiply(BigDecimal.valueOf(2));
                order.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(2)
                        .unitPrice(product.getPrice()).subtotal(subtotal).build());
                total=total.add(subtotal);
                var stock=inventory.findByProductId(product.getId()).orElseThrow();
                stock.setQuantity(stock.getQuantity()-2);inventory.save(stock);
            }
            order.setSubtotal(total);order.setTotalAmount(total);Order saved=orders.saveAndFlush(order);
            for(Pet animal:reserved) {
                animal.setAvailabilityStatus(AvailabilityStatus.PENDING);animal.setReservedOrder(saved);pets.saveAndFlush(animal);
            }
            return saved;
        });
    }
    org.springframework.test.web.servlet.ResultActions postOrder(String body) throws Exception {
        return mvc.perform(post("/api/v1/orders").with(user(caller(customer,"CUSTOMER"))).contentType("application/json").content(body));
    }
    @BeforeEach void setup() { customer=account();pet=sale();authenticate(customer,"CUSTOMER"); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void httpRejectsPetPurchaseAndForgedFieldsWithoutChangingData() throws Exception {
        long before=transactions.count(), orderCount=orders.count();
        var body=Map.of("items",List.of(Map.of("petId",pet.getId(),"quantity",1,"price",0.01)),"shippingAddress","Địa chỉ test","customerId",999,"userId",999,"totalAmount",0.01);
        postOrder(json.writeValueAsString(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message",org.hamcrest.Matchers.containsString("Mua thú cưng chưa khả dụng")));
        var unchanged=pets.findById(pet.getId()).orElseThrow();
        assertEquals(AvailabilityStatus.AVAILABLE,unchanged.getAvailabilityStatus());assertNull(unchanged.getReservedOrder());assertNull(unchanged.getOwner());
        assertEquals(0,new BigDecimal("1500.00").compareTo(unchanged.getPrice()));
        assertEquals(before,transactions.count());assertEquals(orderCount,orders.count());
        mvc.perform(get("/api/v1/orders").with(user(caller(customer,"CUSTOMER")))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/pets/{id}",pet.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE")).andExpect(jsonPath("$.price").value(1500));
    }
    @Test void rejectsMissingUnavailableOwnedAndUnpricedPets() throws Exception {
        long before=orders.count();
        postOrder(json.writeValueAsString(request(item(Long.MAX_VALUE)))).andExpect(status().isBadRequest());
        for (AvailabilityStatus state:List.of(AvailabilityStatus.PENDING,AvailabilityStatus.SOLD,AvailabilityStatus.RESERVED,AvailabilityStatus.UNAVAILABLE)) {
            pet.setAvailabilityStatus(state);pets.save(pet);postOrder(json.writeValueAsString(request(item(pet.getId())))).andExpect(status().isBadRequest());
            assertEquals(state,pets.findById(pet.getId()).orElseThrow().getAvailabilityStatus());
        }
        pet.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);pet.setOwner(customer);pets.save(pet);
        postOrder(json.writeValueAsString(request(item(pet.getId())))).andExpect(status().isBadRequest());
        assertEquals(customer.getId(),pets.findById(pet.getId()).orElseThrow().getOwner().getId());
        pet.setOwner(null);pet.setPrice(BigDecimal.ZERO);pets.save(pet);
        postOrder(json.writeValueAsString(request(item(pet.getId())))).andExpect(status().isBadRequest());
        assertEquals(before,orders.count());assertNull(pets.findById(pet.getId()).orElseThrow().getReservedOrder());
    }
    @Test void invalidItemsHave400IncludingServiceChecks() throws Exception {
        var both=item(pet.getId());both.setProductId(1L);
        var neither=new OrderItemDTO();neither.setQuantity(1);
        var two=item(pet.getId());two.setQuantity(2);
        for(var invalid:List.of(both,neither,two)) {
            postOrder(json.writeValueAsString(request(invalid))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
            authenticate(customer,"CUSTOMER");assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.createOrder(request(invalid)));
        }
        postOrder(json.writeValueAsString(request(item(pet.getId()),item(pet.getId())))).andExpect(status().isBadRequest());
        postOrder("{\"items\":[null],\"shippingAddress\":\"Test\"}").andExpect(status().isBadRequest());
    }
    @Test void expiryReleasesOnlyUnpaidDueOrdersOnce() {
        var order = existingOrder(null, pet);
        order.setExpiresAt(java.time.LocalDateTime.now().plusDays(2));
        orders.save(order);
        assertFalse(service.expireIfDue(order.getId()));
        order.setExpiresAt(java.time.LocalDateTime.now().minusDays(2));
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        orders.save(order);
        assertFalse(service.expireIfDue(order.getId()));
        order.setPaymentStatus(PaymentStatus.PENDING);
        orders.save(order);
        assertTrue(service.expireIfDue(order.getId()));
        assertFalse(service.expireIfDue(order.getId()));
        assertEquals(AvailabilityStatus.AVAILABLE, pets.findById(pet.getId()).orElseThrow().getAvailabilityStatus());
        assertNotNull(orders.findById(order.getId()).orElseThrow().getExpiredAt());
    }

    @Test void onlyCustomerCanCreate() throws Exception {
        String body=json.writeValueAsString(request(item(pet.getId())));
        SecurityContextHolder.clearContext();mvc.perform(post("/api/v1/orders").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        for(String role:List.of("ADMIN","SALES_STAFF","WAREHOUSE_STAFF","VETERINARIAN"))
            mvc.perform(post("/api/v1/orders").with(user(caller(customer,role))).contentType("application/json").content(body)).andExpect(status().isForbidden());
    }
    @Test void mixedPurchaseIsRejectedWithoutChangingStockCartOrPets() throws Exception {
        var category=categories.save(ProductCategory.builder().name("Mixed purchase").build());
        var product=products.save(Product.builder().name("Food").sku(UUID.randomUUID().toString()).category(category).price(BigDecimal.TEN).active(true).build());
        inventory.save(Inventory.builder().product(product).quantity(3).build());
        Cart cart=Cart.builder().customer(customer).build();
        cart.addItem(CartItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(2).build());carts.saveAndFlush(cart);
        var line=new OrderItemDTO();line.setProductId(product.getId());line.setQuantity(2);
        long before=orders.count(), stockTransactions=transactions.count();
        for(var lines:List.of(List.of(line,item(pet.getId())),List.of(item(pet.getId()),line))) {
            var body=request(lines.toArray(OrderItemDTO[]::new));
            postOrder(json.writeValueAsString(body)).andExpect(status().isBadRequest());
            authenticate(customer,"CUSTOMER");
            assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.createOrder(body));
        }
        assertEquals(before,orders.count());assertEquals(stockTransactions,transactions.count());
        assertEquals(3,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var unchanged=carts.findByCustomerId(customer.getId()).orElseThrow();assertEquals(1,unchanged.getItems().size());
            assertEquals(product.getId(),unchanged.getItems().get(0).getProduct().getId());assertEquals(2,unchanged.getItems().get(0).getQuantity());
        });
        var unchanged=pets.findById(pet.getId()).orElseThrow();assertEquals(AvailabilityStatus.AVAILABLE,unchanged.getAvailabilityStatus());
        assertNull(unchanged.getOwner());assertNull(unchanged.getReservedOrder());
    }
    @Test void existingMixedOrderCancelIsIdempotentAfterAnotherReservation() throws Exception {
        var c=categories.save(ProductCategory.builder().name("Food").build());
        var product=products.save(Product.builder().name("Food").sku(UUID.randomUUID().toString()).category(c).price(BigDecimal.TEN).active(true).build());
        inventory.save(Inventory.builder().product(product).quantity(3).build());
        var order=existingOrder(product,pet);assertEquals(1520.0,service.getOrderById(order.getId()).getTotalPrice());
        mvc.perform(get("/api/v1/orders/{id}",order.getId()).with(user(caller(customer,"CUSTOMER"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].petId").value(pet.getId()))
                .andExpect(jsonPath("$.items[0].price").value(1500)).andExpect(jsonPath("$.items[1].productId").value(product.getId()));
        authenticate(customer,"CUSTOMER");
        assertEquals(1,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
        service.cancelOrder(order.getId());assertEquals(AvailabilityStatus.AVAILABLE,pets.findById(pet.getId()).orElseThrow().getAvailabilityStatus());
        var next=existingOrder(null,pet);long stockTransactions=transactions.count();service.cancelOrder(order.getId());
        assertEquals(next.getId(),pets.findById(pet.getId()).orElseThrow().getReservedOrder().getId());
        assertEquals(3,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
        assertEquals(stockTransactions,transactions.count());
    }
    @Test void deliveryAssignsOwnerAndCannotRepeatOrCancel() throws Exception {
        var order=existingOrder(null,pet);authenticate(account(),"ADMIN");
        service.confirmOrder(order.getId());service.processOrder(order.getId());service.shipOrder(order.getId());service.deliverOrder(order.getId());
        var sold=pets.findById(pet.getId()).orElseThrow();assertEquals(customer.getId(),sold.getOwner().getId());assertEquals(AvailabilityStatus.SOLD,sold.getAvailabilityStatus());assertNull(sold.getReservedOrder());
        assertThrows(IllegalStateException.class,()->service.deliverOrder(order.getId()));
        assertThrows(com.petcarehub.exception.InvalidStateTransitionException.class,()->service.cancelOrder(order.getId()));
        mvc.perform(get("/api/v1/pets/{id}",pet.getId())).andExpect(status().isNotFound()).andExpect(jsonPath("$.availabilityStatus").doesNotExist())
                .andExpect(jsonPath("$.name").doesNotExist()).andExpect(jsonPath("$.ownerId").doesNotExist()).andExpect(jsonPath("$.description").doesNotExist());
    }
    @Test void petAndInvalidProductAreRejectedWithoutReservationOrOrder() {
        long before=orders.count();var product=new OrderItemDTO();product.setProductId(Long.MAX_VALUE);product.setQuantity(1);
        assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.createOrder(request(item(pet.getId()),product)));
        assertEquals(before,orders.count());assertEquals(AvailabilityStatus.AVAILABLE,pets.findById(pet.getId()).orElseThrow().getAvailabilityStatus());assertNull(pets.findById(pet.getId()).orElseThrow().getReservedOrder());
    }
    @Test void abnormalReservationCannotPartiallyDeliverOrCancel() {
        Pet second=sale();var order=existingOrder(null,pet,second);authenticate(customer,"ADMIN");
        var bad=pets.findById(second.getId()).orElseThrow();bad.setReservedOrder(null);pets.save(bad);
        assertThrows(IllegalStateException.class,()->service.cancelOrder(order.getId()));
        assertEquals(OrderStatus.PENDING,orders.findById(order.getId()).orElseThrow().getStatus());
        assertEquals(order.getId(),pets.findById(pet.getId()).orElseThrow().getReservedOrder().getId());
        service.confirmOrder(order.getId());service.processOrder(order.getId());service.shipOrder(order.getId());
        assertThrows(IllegalStateException.class,()->service.deliverOrder(order.getId()));
        assertEquals(OrderStatus.SHIPPING,orders.findById(order.getId()).orElseThrow().getStatus());
        assertNull(pets.findById(pet.getId()).orElseThrow().getOwner());
        assertEquals(AvailabilityStatus.PENDING,pets.findById(pet.getId()).orElseThrow().getAvailabilityStatus());
    }
    @Test void concurrentPetPurchasesAreBothRejectedWithoutReservation() throws Exception {
        long before=orders.count(), stockTransactions=transactions.count();
        var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Boolean> buy=()->{authenticate(customer,"CUSTOMER");ready.countDown();try {start.await();service.createOrder(request(item(pet.getId())));return true;}
                catch(com.petcarehub.exception.BadRequestException ex){return false;}finally{SecurityContextHolder.clearContext();}};
            var a=pool.submit(buy);var b=pool.submit(buy);assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            assertFalse(a.get(15,TimeUnit.SECONDS));assertFalse(b.get(15,TimeUnit.SECONDS));
            assertEquals(before,orders.count());assertEquals(stockTransactions,transactions.count());
            var unchanged=pets.findById(pet.getId()).orElseThrow();assertEquals(AvailabilityStatus.AVAILABLE,unchanged.getAvailabilityStatus());
            assertNull(unchanged.getReservedOrder());assertNull(unchanged.getOwner());
        } finally {start.countDown();pool.shutdownNow();}
    }
}
