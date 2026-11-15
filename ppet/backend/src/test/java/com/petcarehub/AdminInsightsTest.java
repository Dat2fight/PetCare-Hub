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
class AdminInsightsTest  {
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
    @Autowired com.petcarehub.service.NotificationDispatch dispatch;
    @Autowired com.petcarehub.service.CustomerReminders reminders;
    @Autowired com.petcarehub.service.JobMonitor monitor;
    static final Set<String> READS=Set.of("GET /dashboard/summary","GET /dashboard/alerts","GET /reports/revenue","GET /reports/rankings","GET /reports/export","GET /system/status","GET /notifications/audience-preview","GET /notifications/history","GET /notifications/templates");
    static final Set<String> WRITES=Set.of("POST /notifications/send","POST /notifications/templates","PUT /notifications/templates/{id}","DELETE /notifications/templates/{id}");
    com.fasterxml.jackson.databind.JsonNode getJson(String path)throws Exception{
        return json.readTree(mvc.perform(get("/api/v1/admin"+path).with(as(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    final Set<String> covered=new HashSet<>();
    com.fasterxml.jackson.databind.JsonNode write(String method,String template,String path,Object body)throws Exception{
        long before=audit.count();
        var response=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1/admin"+path).with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        assertEquals(before+1,audit.count());
        covered.add(method+" "+template);
        return response.getContentAsString().isBlank()?json.createObjectNode():json.readTree(response.getContentAsString());
    }
    @Test void allEndpointsEnforceEveryRoleAndDeclareMethodGuards()throws Exception{
        var expected=new HashSet<>(READS);
        expected.addAll(WRITES);
        Set<String> actual=new HashSet<>();
        mappings.getHandlerMethods().forEach((mapping,handler)->{
            if(!Set.of(com.petcarehub.controller.AdminInsightsController.class,com.petcarehub.controller.AdminNotificationController.class).contains(handler.getBeanType()))return;assertNotNull(handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class));mapping.getPatternValues().forEach(p->mapping.getMethodsCondition().getMethods().forEach(m->actual.add(m.name()+" "+p.substring("/api/v1/admin".length()))));
        }
        );
        assertEquals(expected,actual);
        for(var actor:List.of(admin,sales,vet,customer,warehouse))for(String endpoint:expected){
            String[] p=endpoint.split(" ",2);
            var req=request(org.springframework.http.HttpMethod.valueOf(p[0]),"/api/v1/admin"+p[1].replace("{id}","987654321")).with(as(actor)).contentType("application/json").content("{").param("from",LocalDate.now(clock).toString()).param("to",LocalDate.now(clock).toString()).param("group","ROLE").param("role","CUSTOMER");
            int status=mvc.perform(req).andReturn().getResponse().getStatus();
            if(actor==admin)assertTrue(Set.of(200,400,404).contains(status),endpoint+" -> "+status);
            else assertEquals(403,status,endpoint);
        }
    }
    Order revenueOrder(int amount,LocalDateTime paid,PaymentStatus payment,OrderStatus status,boolean withPet){
        var o=Order.builder().orderNumber(UUID.randomUUID().toString()).customer(customer).subtotal(BigDecimal.valueOf(amount)).totalAmount(BigDecimal.valueOf(amount)).paymentMethod("COD").paymentStatus(payment).status(status).paidAt(paid).build();
        o.addItem(OrderItem.builder().itemType(withPet?OrderItemType.PET:OrderItemType.PRODUCT).pet(withPet?pet:null).product(withPet?null:product).quantity(1).unitPrice(BigDecimal.valueOf(amount)).subtotal(BigDecimal.valueOf(amount)).build());
        return save(o);
    }
    @Test void revenueUsesPaidTimeExcludesRefundsAndFillsGaps()throws Exception{
        LocalDate day=LocalDate.now(clock).minusDays(3);
        revenueOrder(100,day.atStartOfDay(),PaymentStatus.COMPLETED,OrderStatus.DELIVERED,false);
        revenueOrder(40,day.plusDays(2).atTime(23,59),PaymentStatus.COMPLETED,OrderStatus.DELIVERED,true);
        revenueOrder(900,day.atTime(12,0),PaymentStatus.REFUNDED,OrderStatus.DELIVERED,false);
        revenueOrder(900,day.atTime(12,0),PaymentStatus.COMPLETED,OrderStatus.CANCELLED,false);
        revenueOrder(900,day.atTime(12,0),PaymentStatus.PENDING,OrderStatus.PENDING,false);
        revenueOrder(900,day.plusDays(3).atStartOfDay(),PaymentStatus.COMPLETED,OrderStatus.DELIVERED,false);
        revenueOrder(70,day.minusDays(1).atTime(12,0),PaymentStatus.COMPLETED,OrderStatus.DELIVERED,false);
        String period="from="+day+"&to="+day.plusDays(2);
        var summary=getJson("/dashboard/summary?period=custom&"+period);
        assertEquals(140,summary.at("/metrics/revenue/current").intValue());
        assertEquals(70,summary.at("/metrics/revenue/previous").intValue());
        assertEquals(100,summary.at("/metrics/revenue/percent").intValue());
        var revenue=getJson("/reports/revenue?"+period);
        assertEquals(3,revenue.size());
        assertEquals(0,revenue.get(1).get("revenue").intValue());
        assertEquals(40,revenue.get(2).get("revenue").intValue());
        var top=getJson("/reports/rankings?"+period);
        assertEquals(1,top.get("products").size());
        assertEquals(1,top.at("/products/0/quantity").intValue());
        var empty=getJson("/dashboard/summary?period=custom&from=2001-01-01&to=2001-01-02");
        assertEquals(0,empty.at("/metrics/revenue/current").intValue());
        assertEquals(0,empty.at("/metrics/revenue/percent").intValue());
        mvc.perform(get("/api/v1/admin/reports/revenue?from=2025-01-01&to=2026-12-01").with(as(admin))).andExpect(status().isBadRequest());
        var fixed=Clock.fixed(Instant.parse("2026-11-14T17:05:00Z"),ZoneId.of("Asia/Ho_Chi_Minh"));
        assertEquals(LocalDate.of(2026,11,15),com.petcarehub.service.ReportPeriod.of("today",null,null,fixed).from());
    }
    @Test void exportIsUtf8FormulaSafeAndAudited()throws Exception{
        product.setName("=SUM(1,2)");
        em.flush();
        long before=audit.count();
        var pending=mvc.perform(get("/api/v1/admin/reports/export?kind=inventory").with(as(admin))).andExpect(request().asyncStarted()).andReturn();
        var response=mvc.perform(asyncDispatch(pending)).andExpect(status().isOk()).andReturn().getResponse();
        assertEquals(before+1,audit.count());
        String csv=response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF"));
        assertTrue(csv.contains("\"'=SUM(1,2)\""));
        assertFalse(csv.contains("password"));
        for(String value:List.of("=x","+x","-x","@x","\tdata","\rdata","  =x"))assertTrue(com.petcarehub.service.AdminCsvExport.cell(value).startsWith("\"'"));
    }
    @Test void notificationWritesAuditExactlyOnceAndDurableWorkerDeduplicates()throws Exception{
        var body=new LinkedHashMap<String,Object>(Map.of("code","TEST_GENERAL","purpose","GENERAL","title","Chào {firstName}","body","Thông báo tiếng Việt","active",true));
        long template=write("POST","/notifications/templates","/notifications/templates",body).get("id").asLong();
        body.put("body","Thông tin mới");
        write("PUT","/notifications/templates/{id}","/notifications/templates/"+template,body);
        var preview=getJson("/notifications/audience-preview?group=SINGLE&userId="+customer.getId());
        var request=new LinkedHashMap<String,Object>(Map.of("group","SINGLE","userId",customer.getId(),"templateId",template,"requestKey",UUID.randomUUID().toString(),"audienceToken",preview.get("audienceToken").asText()));
        long batch=write("POST","/notifications/send","/notifications/send",request).get("id").asLong();
        long before=em.createQuery("select count(n) from Notification n",Long.class).getSingleResult();
        assertEquals(1,dispatch.process(batch));
        assertEquals(0,dispatch.process(batch));
        write("POST","/notifications/send","/notifications/send",request);
        assertEquals(before+1,em.createQuery("select count(n) from Notification n",Long.class).getSingleResult());
        assertEquals("COMPLETED",em.find(NotificationBatch.class,batch).getStatus());
        request.put("requestKey",UUID.randomUUID().toString());
        mvc.perform(post("/api/v1/admin/notifications/send").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(request))).andExpect(status().isConflict());
        write("DELETE","/notifications/templates/{id}","/notifications/templates/"+template,Map.of("reason","Test cleanup"));
        assertEquals(WRITES,covered);
    }
    @Test void audiencesAndTemplateVariablesAreValidated()throws Exception{
        save(LoyaltyAccount.builder().customer(customer).totalPoints(5000).build());
        assertEquals(1,getJson("/notifications/audience-preview?group=TIER&tier=GOLD").get("count").intValue());
        for(String message:List.of("<script>alert(1)</script>","{unknown}","${java.lang.Runtime}"))mvc.perform(post("/api/v1/admin/notifications/templates").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("code","BAD","purpose","GENERAL","title","Test","body",message)))).andExpect(status().isBadRequest());
        assertFalse(com.petcarehub.service.NotificationText.render("Hi {firstName}",Map.of("firstName","<script>"),200).contains("<script>"));
        for(int i=0;i<501;i++)account(RoleType.CUSTOMER);
        var preview=getJson("/notifications/audience-preview?group=ROLE&role=CUSTOMER");
        assertTrue(preview.get("overLimit").asBoolean());
        mvc.perform(post("/api/v1/admin/notifications/send").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("group","ROLE","role","CUSTOMER","title","Test","message","Message","requestKey",UUID.randomUUID().toString(),"audienceToken",preview.get("audienceToken").asText())))).andExpect(status().isBadRequest());
    }
    @Test void remindersAreIdempotentAndSystemStatusIsSanitized()throws Exception{
        var soon=LocalDateTime.now(clock).plusHours(1).withSecond(0).withNano(0);
        appointment.setAppointmentDate(soon.toLocalDate());
        appointment.setStartTime(soon.toLocalTime());
        appointment.setEndTime(soon.plusMinutes(30).toLocalTime());
        save(Vaccination.builder().pet(pet).veterinarian(vet).vaccineName("Test vaccine").dateAdministered(LocalDate.now(clock).minusMonths(3)).nextDueDate(LocalDate.now(clock).plusDays(3)).build());
        em.flush();
        assertEquals(2,reminders.run());
        assertEquals(0,reminders.run());
        Long run=monitor.start("CUSTOMER_REMINDERS");
        monitor.finish(run,0,true);
        var result=getJson("/system/status");
        assertEquals("CONNECTED",result.get("database").asText());
        assertTrue(result.toString().contains("JOB_EXECUTION_FAILED"));
        for(String forbidden:List.of("jdbc:","password","secret","stackTrace"))assertFalse(result.toString().contains(forbidden));
    }
}
