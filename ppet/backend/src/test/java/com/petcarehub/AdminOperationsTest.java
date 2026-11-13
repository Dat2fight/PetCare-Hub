package com.petcarehub;
import com.petcarehub.entity.*;
import com.petcarehub.entity.Order;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class AdminOperationsTest {
    @Autowired MockMvc mvc;@Autowired EntityManager em;@Autowired RoleRepository roles;@Autowired AdminAuditRepository audit;@Autowired RequestMappingHandlerMapping mappings;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    User admin,sales,vet,otherVet,customer,warehouse;Product product;Inventory stock;Order order;Appointment appointment;Review review;Pet pet;
    <T>T save(T value){em.persist(value);em.flush();return value;}
    User account(RoleType type){var role=roles.findByName(type).orElseGet(()->roles.save(Role.builder().name(type).build()));String n=UUID.randomUUID().toString();return save(User.builder().username(n).email(n+"@example.test").firstName("Test").lastName("Ops").passwordHash("unused").roles(Set.of(role)).build());}
    org.springframework.test.web.servlet.request.RequestPostProcessor as(User actor){return user(UserDetailsImpl.build(actor));}
    @BeforeEach void fixture(){
        admin=account(RoleType.ADMIN);sales=account(RoleType.SALES_STAFF);vet=account(RoleType.VETERINARIAN);otherVet=account(RoleType.VETERINARIAN);customer=account(RoleType.CUSTOMER);warehouse=account(RoleType.WAREHOUSE_STAFF);
        var category=save(ProductCategory.builder().name("Ops").build());product=save(Product.builder().name("Food").sku(UUID.randomUUID().toString()).price(BigDecimal.TEN).category(category).build());stock=save(Inventory.builder().product(product).quantity(5).build());
        order=Order.builder().orderNumber(UUID.randomUUID().toString()).customer(customer).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).paymentMethod("COD").build();
        order.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());save(order);
        var species=save(Species.builder().name("Ops dog").build());pet=save(Pet.builder().name("Pup").price(BigDecimal.TEN).species(species).owner(customer).build());
        var service=save(ServiceEntity.builder().name("Checkup").serviceType(ServiceType.VETERINARY).durationMinutes(30).price(BigDecimal.TEN).build());
        appointment=save(Appointment.builder().customer(customer).pet(pet).service(service).staff(vet).appointmentDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(10,0)).endTime(LocalTime.of(10,30)).build());
        review=save(Review.builder().customer(customer).reviewableType("PRODUCT").reviewableId(product.getId()).rating(4).comment("<script>alert('test')</script>").moderationState("PENDING").build());
    }
    static final Set<String> READS=Set.of("GET /orders","GET /orders/{id}","GET /orders/{id}/print","GET /orders/{id}/notes","GET /orders/{id}/payments","GET /inventory","GET /inventory/transactions","GET /appointments","GET /appointments/{id}","GET /appointments/{id}/eligible-staff","GET /reviews","GET /reviews/count");
    static final Set<String> WRITES=Set.of("POST /orders/{id}/transition","POST /orders/{id}/request-refund","POST /orders/{id}/mark-refunded","POST /orders/{id}/notes","POST /inventory/movements","POST /appointments/{id}/assign","POST /appointments/{id}/status","POST /reviews/{id}/moderate");
    @Test void roleMatrixAndCompleteEndpointInventory() throws Exception {
        Set<String> expected=new HashSet<>(READS);expected.addAll(WRITES);Set<String> actual=new HashSet<>();
        mappings.getHandlerMethods().forEach((mapping,handler)->{if(handler.getBeanType()!=com.petcarehub.controller.AdminOperationsController.class)return;assertNotNull(handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class));mapping.getPatternValues().forEach(p->mapping.getMethodsCondition().getMethods().forEach(m->actual.add(m.name()+" "+p.substring("/api/v1/admin".length()))));});assertEquals(expected,actual);
        for(var actor:List.of(admin,sales,vet,customer,warehouse))for(String endpoint:expected){String[] parts=endpoint.split(" ");String path=parts[1];Long id=path.startsWith("/appointments")?appointment.getId():path.startsWith("/reviews")?review.getId():order.getId();
            boolean allowed=actor==admin || actor==sales&&(path.startsWith("/orders")||path.startsWith("/reviews")||path.startsWith("/inventory")||path.startsWith("/appointments")&&!path.endsWith("/assign")&&!path.endsWith("/eligible-staff")) || actor==warehouse&&path.startsWith("/inventory") || actor==vet&&path.startsWith("/appointments")&&!path.endsWith("/assign")&&!path.endsWith("/eligible-staff");
            var method=org.springframework.http.HttpMethod.valueOf(parts[0]);String url="/api/v1/admin"+path.replace("{id}",id.toString());
            mvc.perform(request(method,url)).andExpect(status().isUnauthorized());
            int status=mvc.perform(request(method,url).with(as(actor)).contentType("application/json").content("{" )).andReturn().getResponse().getStatus();
            if(!allowed)assertEquals(403,status,endpoint);else assertTrue(Set.of(200,400,404).contains(status),endpoint+" returned "+status);
        }
    }
    Set<String> covered=new HashSet<>();
    void write(String template,Long id,Object body,User actor) throws Exception {long before=audit.count();mvc.perform(post("/api/v1/admin"+template.replace("{id}",id.toString())).with(as(actor)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isOk());assertEquals(before+1,audit.count(),template);covered.add("POST "+template);}
    @Test void everyOperationWritesOneAuditAndPaymentReplayDoesNotDuplicateMoney() throws Exception {
        for(String action:List.of("confirm","process","ship","deliver"))write("/orders/{id}/transition",order.getId(),Map.of("action",action),sales);
        assertEquals(PaymentStatus.COMPLETED,order.getPaymentStatus());
        write("/orders/{id}/request-refund",order.getId(),Map.of("body","Approved full cash refund"),sales);
        var refund=Map.of("reference","receipt-123","reason","Paid back cash");write("/orders/{id}/mark-refunded",order.getId(),refund,sales);write("/orders/{id}/mark-refunded",order.getId(),refund,sales);
        assertEquals(2L,em.createQuery("select count(e) from OrderPaymentEvent e where e.orderId=:id",Long.class).setParameter("id",order.getId()).getSingleResult());
        write("/orders/{id}/notes",order.getId(),Map.of("body","PRIVATE_INTERNAL_NOTE"),sales);
        String customerBody=mvc.perform(get("/api/v1/orders/"+order.getId()).with(as(customer))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertFalse(customerBody.contains("PRIVATE_INTERNAL_NOTE"));
        write("/inventory/movements",0L,Map.of("productId",product.getId(),"type","STOCK_OUT","quantity",1,"reason","Damaged"),warehouse);assertEquals(4,stock.getQuantity());
        write("/appointments/{id}/assign",appointment.getId(),Map.of("staffId",otherVet.getId(),"reason","Reschedule staff"),admin);
        write("/appointments/{id}/status",appointment.getId(),Map.of("status","CANCELLED","reason","Customer agreed"),otherVet);
        write("/reviews/{id}/moderate",review.getId(),Map.of("action","APPROVED"),sales);assertTrue(review.getApproved());
        write("/reviews/{id}/moderate",review.getId(),Map.of("action","HIDDEN","reason","Hidden"),sales);assertFalse(review.getApproved());assertEquals(WRITES,covered);
    }
    @Test void invalidTransitionsScopeStockAndAssignmentsAreRejected() throws Exception {
        mvc.perform(post("/api/v1/admin/orders/"+order.getId()+"/transition").with(as(sales)).contentType("application/json").content("{\"action\":\"ship\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/admin/orders/"+order.getId()+"/mark-refunded").with(as(sales)).contentType("application/json").content("{\"reference\":\"invalid\",\"reason\":\"test\"}")).andExpect(status().isConflict());
        for(int quantity:new int[]{0,-1,99})mvc.perform(post("/api/v1/admin/inventory/movements").with(as(warehouse)).contentType("application/json").content(json.writeValueAsBytes(Map.of("productId",product.getId(),"type","STOCK_OUT","quantity",quantity,"reason","test")))).andExpect(result->assertTrue(Set.of(400,409).contains(result.getResponse().getStatus())));
        assertEquals(5,stock.getQuantity());
        mvc.perform(get("/api/v1/admin/appointments/"+appointment.getId()).with(as(otherVet))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/appointments?staff="+vet.getId()).with(as(otherVet))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/admin/appointments/"+appointment.getId()+"/assign").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("staffId",sales.getId(),"reason","test")))).andExpect(status().isBadRequest());
        otherVet.setEnabled(false);em.flush();
        mvc.perform(post("/api/v1/admin/appointments/"+appointment.getId()+"/assign").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("staffId",otherVet.getId(),"reason","test")))).andExpect(status().isBadRequest());
        otherVet.setEnabled(true);em.flush();
        save(Appointment.builder().customer(customer).pet(pet).service(appointment.getService()).staff(otherVet).appointmentDate(appointment.getAppointmentDate()).startTime(LocalTime.of(10,0)).endTime(LocalTime.of(11,0)).build());
        mvc.perform(post("/api/v1/admin/appointments/"+appointment.getId()+"/assign").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("staffId",otherVet.getId(),"reason","test")))).andExpect(status().isConflict());
        assertEquals(vet.getId(),appointment.getStaff().getId());
    }
    @Test void cancellationReleasesMixedOrderAndReviewModerationAffectsPublicStats() throws Exception {
        pet.setOwner(null);pet.setAvailabilityStatus(AvailabilityStatus.PENDING);pet.setReservedOrder(order);pet.setPrice(BigDecimal.TEN);
        order.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(pet).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());em.flush();
        write("/orders/{id}/transition",order.getId(),Map.of("action","cancel","reason","Customer cancelled"),sales);
        assertEquals(AvailabilityStatus.AVAILABLE,pet.getAvailabilityStatus());assertNull(pet.getReservedOrder());assertEquals(6,stock.getQuantity());
        mvc.perform(post("/api/v1/admin/reviews/"+review.getId()+"/moderate").with(as(sales)).contentType("application/json").content("{\"action\":\"REJECTED\"}")).andExpect(status().isBadRequest());
        String summary="/api/v1/reviews/summary?targetType=PRODUCT&targetId="+product.getId();
        mvc.perform(get(summary)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/api/v1/reviews/"+review.getId())).andExpect(status().isNotFound());
        write("/reviews/{id}/moderate",review.getId(),Map.of("action","APPROVED"),sales);
        mvc.perform(get(summary)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.average").value(4.0));
        write("/reviews/{id}/moderate",review.getId(),Map.of("action","HIDDEN"),sales);
        mvc.perform(get(summary)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/api/v1/reviews/"+review.getId())).andExpect(status().isNotFound());
    }
    @Test void queuesEnforcePagingFiltersAndSortWhitelist() throws Exception {
        mvc.perform(get("/api/v1/admin/inventory?size=10000&low=true").with(as(sales)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(100)).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/orders?keyword=%25").with(as(sales)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/orders?sort=customer.passwordHash,asc").with(as(sales))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/orders?from=2026-12-31&to=2026-01-01").with(as(sales))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/appointments").with(as(otherVet)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/admin/appointments").with(as(vet)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/reviews?status=PENDING&type=PRODUCT&target="+product.getId()).with(as(sales)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/admin/reviews/count").with(as(sales)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(1));
    }

}
