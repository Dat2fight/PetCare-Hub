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
class AdminContentSettingsTest  {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired RoleRepository roles;
    @Autowired AdminAuditRepository audit;
    @Autowired RequestMappingHandlerMapping mappings;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    User admin,sales,vet,otherVet,customer,warehouse;
    Product product;
    Inventory stock;
    Order order;
    Appointment appointment;
    Review review;
    Pet pet;
    <T>T save(T value) {
        em.persist(value);
        em.flush();
        return value;
    }
    User account(RoleType type) {
        var role=roles.findByName(type).orElseGet(()->roles.save(Role.builder().name(type).build()));
        String n=UUID.randomUUID().toString();
        return save(User.builder().username(n).email(n+"@example.test").firstName("Test").lastName("Ops").passwordHash("unused").roles(Set.of(role)).build());
    }
    org.springframework.test.web.servlet.request.RequestPostProcessor as(User actor) {
        return user(UserDetailsImpl.build(actor));
    }
    @BeforeEach void fixture() {
        admin=account(RoleType.ADMIN);
        sales=account(RoleType.SALES_STAFF);
        vet=account(RoleType.VETERINARIAN);
        otherVet=account(RoleType.VETERINARIAN);
        customer=account(RoleType.CUSTOMER);
        warehouse=account(RoleType.WAREHOUSE_STAFF);
        var category=save(ProductCategory.builder().name("Ops").build());
        product=save(Product.builder().name("Food").sku(UUID.randomUUID().toString()).price(BigDecimal.TEN).category(category).build());
        stock=save(Inventory.builder().product(product).quantity(5).build());
        order=Order.builder().orderNumber(UUID.randomUUID().toString()).customer(customer).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).paymentMethod("COD").build();
        order.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());
        save(order);
        var species=save(Species.builder().name("Ops dog").build());
        pet=save(Pet.builder().name("Pup").price(BigDecimal.TEN).species(species).owner(customer).build());
        var service=save(ServiceEntity.builder().name("Checkup").serviceType(ServiceType.VETERINARY).durationMinutes(30).price(BigDecimal.TEN).build());
        appointment=save(Appointment.builder().customer(customer).pet(pet).service(service).staff(vet).appointmentDate(LocalDate.now().plusDays(3)).startTime(LocalTime.of(10,0)).endTime(LocalTime.of(10,30)).build());
        review=save(Review.builder().customer(customer).reviewableType("PRODUCT").reviewableId(product.getId()).rating(4).comment("<script>alert('test')</script>").moderationState("PENDING").build());
    }
    @Autowired Clock clock;
    @Autowired com.petcarehub.service.DataExportWorker exports;
    static final Set<String> READS=Set.of("GET /content","GET /content/{id}","GET /content/{id}/revisions","GET /settings","GET /data-exports","GET /returns");
    static final Set<String> WRITES=Set.of("POST /content","PUT /content/{id}","DELETE /content/{id}","POST /content/{id}/publish","POST /content/{id}/unpublish","POST /content/{id}/restore","PUT /settings","POST /data-exports","POST /data-exports/{id}/download","POST /returns/{id}/transition");
    static final Set<String> covered=new HashSet<>();
    @BeforeAll static void resetCoverage(){
        covered.clear();
    }
    @AfterAll static void auditCoverage(){
        assertEquals(WRITES,covered);
    }
    com.fasterxml.jackson.databind.JsonNode getJson(String path)throws Exception{
        return json.readTree(mvc.perform(get("/api/v1/admin"+path).with(as(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    com.fasterxml.jackson.databind.JsonNode write(String method,String template,String path,Object body)throws Exception{
        long before=audit.count();
        var response=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1/admin"+path).with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        assertEquals(before+1,audit.count());
        covered.add(method+" "+template);
        return response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8).isBlank()?json.createObjectNode():json.readTree(response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
    @Test void roleMatrixAndPrivateContentScope()throws Exception{
        var expected=new HashSet<>(READS);
        expected.addAll(WRITES);
        Set<String> actual=new HashSet<>();
        mappings.getHandlerMethods().forEach((m,h)->{
            if(!Set.of(com.petcarehub.controller.AdminContentController.class,com.petcarehub.controller.AdminSettingsController.class,com.petcarehub.controller.AdminDataExportsController.class,com.petcarehub.controller.AdminReturnsController.class).contains(h.getBeanType()))return;assertNotNull(h.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class));m.getPatternValues().forEach(p->m.getMethodsCondition().getMethods().forEach(v->actual.add(v+" "+p.substring("/api/v1/admin".length()))));
        }
        );
        assertEquals(expected,actual);
        for(var actor:List.of(admin,sales,warehouse,vet,customer))for(String endpoint:expected){
            var parts=endpoint.split(" ",2);
            int status=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(parts[0]),"/api/v1/admin"+parts[1].replace("{id}","987654321")).with(as(actor)).contentType("application/json").content("{")).andReturn().getResponse().getStatus();
            boolean allowed=actor==admin||actor==sales&&(parts[1].startsWith("/content")||parts[1].startsWith("/returns"));
            if(allowed)assertTrue(Set.of(200,400,404).contains(status),endpoint+" "+status);
            else assertEquals(403,status,endpoint);
        }
    }
    @Test void draftsStayPrivateAndRestoreDoesNotRepublish()throws Exception{
        var body=new LinkedHashMap<String,Object>(Map.of("slug","qa-article","kind","ARTICLE","title","Bài viết tiếng Việt","body","# An toàn\n<script>alert(1)</script>"));
        long id=write("POST","/content","/content",body).get("id").asLong();
        mvc.perform(get("/api/v1/content/qa-article")).andExpect(status().isNotFound());
        write("POST","/content/{id}/publish","/content/"+id+"/publish",Map.of());
        long revision=getJson("/content/"+id+"/revisions").at("/content/0/id").asLong();
        body.put("title","Bản nháp bí mật");
        write("PUT","/content/{id}","/content/"+id,body);
        mvc.perform(get("/api/v1/content/qa-article")).andExpect(jsonPath("$.content.title").value("Bài viết tiếng Việt"));
        mvc.perform(get("/api/v1/content?kind=ARTICLE")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Bản nháp bí mật"))));
        write("POST","/content/{id}/restore","/content/"+id+"/restore",Map.of("revisionId",revision));
        assertEquals("Bài viết tiếng Việt",getJson("/content/"+id).get("title").asText());
        mvc.perform(post("/api/v1/admin/content").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isConflict());
        write("POST","/content/{id}/unpublish","/content/"+id+"/unpublish",Map.of());
        mvc.perform(get("/api/v1/content/qa-article")).andExpect(status().isNotFound());
        write("DELETE","/content/{id}","/content/"+id,Map.of("reason","Cleanup"));
    }
    @Test void publicationFiltersUnavailableFeaturedItemsAndKeepsOnlyTwentyRevisions()throws Exception{
        var salePet=save(Pet.builder().name("Featured pup").price(BigDecimal.TEN).species(pet.getSpecies()).availabilityStatus(AvailabilityStatus.AVAILABLE).build());
        var body=new LinkedHashMap<String,Object>(Map.of("slug","home","kind","PAGE","title","Trang chủ","body","Chào mừng","configuration",Map.of("productIds",List.of(product.getId()),"petIds",List.of(salePet.getId()))));
        long id=write("POST","/content","/content",body).get("id").asLong();
        for(int i=0;i<22;i++){
            body.put("title","Phiên bản "+i);
            write("PUT","/content/{id}","/content/"+id,body);
        }
        assertEquals(20,getJson("/content/"+id+"/revisions").get("totalElements").asInt());
        write("POST","/content/{id}/publish","/content/"+id+"/publish",Map.of());
        mvc.perform(get("/api/v1/content/featured")).andExpect(jsonPath("$.products.length()").value(1));
        mvc.perform(get("/api/v1/content/featured")).andExpect(jsonPath("$.pets[0].speciesName").value(pet.getSpecies().getName()));
        salePet.setPrice(new BigDecimal("12345"));
        em.flush();
        mvc.perform(get("/api/v1/content/featured")).andExpect(jsonPath("$.pets[0].price").value(12345));
        salePet.setAvailabilityStatus(AvailabilityStatus.UNAVAILABLE);
        product.setActive(false);
        em.flush();
        mvc.perform(get("/api/v1/content/featured")).andExpect(jsonPath("$.products.length()").value(0)).andExpect(jsonPath("$.pets.length()").value(0));
    }
    @Test void settingsChangeCheckoutAndCancellationWithoutRepricingExistingOrders()throws Exception{
        BigDecimal oldTotal=order.getTotalAmount();
        write("PUT","/settings","/settings",Map.of("shippingFee",12345,"freeShippingFrom",999999,"holdHours",6,"cancellationHours",72,"returnDays",10));
        mvc.perform(post("/api/v1/orders/quote").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(Map.of("items",List.of(Map.of("productId",product.getId(),"quantity",1)))))).andExpect(status().isOk()).andExpect(jsonPath("$.shippingFee").value(12345));
        assertEquals(oldTotal,em.find(Order.class,order.getId()).getTotalAmount());
        for(var body:List.of(Map.of("shippingFee",-1),Map.of("holdHours",0),Map.of("returnDays",91),Map.of("JWT_SECRET",1)))mvc.perform(put("/api/v1/admin/settings").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());
        assertTrue(em.createQuery("from AdminAuditLog",AdminAuditLog.class).getResultStream().anyMatch(a->a.getAction().equals("SYSTEM_SETTINGS_UPDATED")&&a.getDetails().contains("12345")));
        appointment.setAppointmentDate(LocalDate.now(clock).plusDays(1));
        em.flush();
        mvc.perform(get("/api/v1/appointments/"+appointment.getId()).with(as(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.cancellable").value(false));
        var salePet=save(Pet.builder().species(pet.getSpecies()).name("Sale pet").price(BigDecimal.TEN).availabilityStatus(AvailabilityStatus.AVAILABLE).build());
        var placed=json.readTree(mvc.perform(post("/api/v1/orders").with(as(customer)).header("Idempotency-Key",UUID.randomUUID()).contentType("application/json").content(json.writeValueAsBytes(Map.of("items",List.of(Map.of("productId",product.getId(),"quantity",1),Map.of("petId",salePet.getId(),"quantity",1)),"shipping",ShippingFixture.valid(),"paymentMethod","COD")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        var snapshot=em.find(Order.class,placed.get("id").asLong());
        var expires=snapshot.getExpiresAt();
        assertEquals(10,snapshot.getReturnWindowDays());
        assertEquals(0,snapshot.getShippingFee().compareTo(BigDecimal.valueOf(12345)));
        assertTrue(Duration.between(LocalDateTime.now(clock),expires).toMinutes()>=359);
        write("PUT","/settings","/settings",Map.of("shippingFee",0,"holdHours",1,"returnDays",1));
        assertEquals(expires,snapshot.getExpiresAt());
        assertEquals(10,snapshot.getReturnWindowDays());
        assertEquals(0,snapshot.getShippingFee().compareTo(BigDecimal.valueOf(12345)));
    }
    @Test void exportOwnerOnlyOneTimeAndExpiredExportsCannotDownload()throws Exception{
        var body=Map.of("kind","customers","requestKey",UUID.randomUUID().toString());
        long id=write("POST","/data-exports","/data-exports",body).get("id").asLong();
        assertEquals(id,write("POST","/data-exports","/data-exports",body).get("id").asLong());
        assertTrue(exports.process()>0);
        em.flush();
        var another=account(RoleType.ADMIN);
        mvc.perform(post("/api/v1/admin/data-exports/"+id+"/download").with(as(another))).andExpect(status().isNotFound());
        long before=audit.count();
        var response=mvc.perform(post("/api/v1/admin/data-exports/"+id+"/download").with(as(admin))).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.startsWith("\uFEFF")));
        assertEquals(before+1,audit.count());
        covered.add("POST /data-exports/{id}/download");
        assertNull(em.find(DataExportJob.class,id).getPayload());
        mvc.perform(post("/api/v1/admin/data-exports/"+id+"/download").with(as(admin))).andExpect(status().isConflict());
        var job=em.find(DataExportJob.class,id);
        job.setStatus("READY");
        job.setExpiresAt(LocalDateTime.now(clock).minusSeconds(1));
        job.setPayload("secret expired file");
        em.flush();
        mvc.perform(post("/api/v1/admin/data-exports/"+id+"/download").with(as(admin))).andExpect(status().isConflict());
        exports.process();
        em.refresh(job);
        assertNull(job.getPayload());
    }
    @Test void returnOwnershipStateMachineAndFinancialBoundaries()throws Exception{
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaidAt(LocalDateTime.now(clock));
        order.setDeliveredAt(LocalDateTime.now(clock));
        em.flush();
        var body=new LinkedHashMap<String,Object>(Map.of("orderId",order.getId(),"itemId",order.getItems().get(0).getId(),"quantity",1,"reason","Hàng không phù hợp","requestKey",UUID.randomUUID().toString()));
        mvc.perform(post("/api/v1/returns").with(as(account(RoleType.CUSTOMER))).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isNotFound());
        long id=json.readTree(mvc.perform(post("/api/v1/returns").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).get("id").asLong();
        mvc.perform(post("/api/v1/returns").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(jsonPath("$.id").value(id));
        body.put("requestKey",UUID.randomUUID().toString());
        mvc.perform(post("/api/v1/returns").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/returns").with(as(account(RoleType.CUSTOMER)))).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/v1/admin/returns/"+id+"/transition").with(as(sales)).contentType("application/json").content("{\"action\":\"refund\",\"reason\":\"Wrong sequence\",\"amount\":10,\"reference\":\"test\"}")).andExpect(status().isConflict());
        write("POST","/returns/{id}/transition","/returns/"+id+"/transition",Map.of("action","approve","reason","Chấp nhận"));
        assertEquals(5,stock.getQuantity());
        write("POST","/returns/{id}/transition","/returns/"+id+"/transition",Map.of("action","receive","reason","Đã nhận thực tế"));
        assertEquals(6,stock.getQuantity());
        write("POST","/returns/{id}/transition","/returns/"+id+"/transition",Map.of("action","receive","reason","Retry"));
        assertEquals(6,stock.getQuantity());
        mvc.perform(post("/api/v1/admin/returns/"+id+"/transition").with(as(admin)).contentType("application/json").content("{\"action\":\"refund\",\"reason\":\"Too much\",\"amount\":11,\"reference\":\"OVER\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/orders/"+order.getId()+"/request-refund").with(as(admin)).contentType("application/json").content("{\"body\":\"Không hoàn trùng\"}")).andExpect(status().isConflict());
        write("POST","/returns/{id}/transition","/returns/"+id+"/transition",Map.of("action","refund","reason","Đã trả tiền mặt","amount",10,"reference","CASH-TEST"));
        write("POST","/returns/{id}/transition","/returns/"+id+"/transition",Map.of("action","refund","reason","Retry","amount",10,"reference","CASH-TEST"));
        assertEquals(0,order.getReturnedAmount().compareTo(BigDecimal.TEN));
        assertEquals(1L,em.createQuery("select count(e) from OrderPaymentEvent e where e.orderId=:id",Long.class).setParameter("id",order.getId()).getSingleResult());
        assertEquals(0,getJson("/reports/revenue?from="+LocalDate.now(clock)+"&to="+LocalDate.now(clock)).get(0).get("revenue").decimalValue().signum());
    }
    @Test void returnsRejectExpiredOrdersAndNonProductItems()throws Exception{
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setDeliveredAt(LocalDateTime.now(clock).minusDays(8));
        em.flush();
        var body=new LinkedHashMap<String,Object>(Map.of("orderId",order.getId(),"itemId",order.getItems().get(0).getId(),"quantity",1,"reason","Return","requestKey",UUID.randomUUID().toString()));
        mvc.perform(post("/api/v1/returns").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isConflict());
        order.setDeliveredAt(LocalDateTime.now(clock));
        order.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(pet).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());
        em.flush();
        body.put("itemId",order.getItems().get(1).getId());
        mvc.perform(post("/api/v1/returns").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());
    }
}
