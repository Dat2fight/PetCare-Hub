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
class AdminCatalogTest  {
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
    @Autowired com.petcarehub.service.BusinessSchedule businessSchedule;
    @Autowired java.time.Clock clock;
    final Set<String> covered=new HashSet<>();
    static final Set<String> READS=Set.of("GET /catalog/products/low-stock","GET /catalog/{resource}","GET /catalog/{resource}/{id}","GET /pet-support/{id}","GET /schedules/{kind}","GET /schedule-affected","GET /staff-performance","GET /loyalty-rules","GET /promotion-usages/{id}","GET /subscriptions","GET /campaign-audience");
    static final Set<String> WRITES=Set.of("POST /catalog/{resource}","PUT /catalog/{resource}/{id}","DELETE /catalog/{resource}/{id}","POST /catalog/products/bulk-preview","POST /catalog/products/bulk-apply","POST /catalog/products/csv-preview","POST /catalog/products/csv-apply","POST /catalog/pets/{id}/release-reservation","POST /schedules/{kind}","PUT /schedules/{kind}/{id}","DELETE /schedules/{kind}/{id}","POST /schedule-cancel","PUT /loyalty-rules","POST /loyalty/{userId}/adjust","POST /subscriptions/{id}","POST /campaigns");
    com.fasterxml.jackson.databind.JsonNode write(String method,String template,String path,Object body) throws Exception  {
        long before=audit.count();
        var result=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1/admin"+path).with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().is2xxSuccessful()).andReturn();
        assertEquals(before+1,audit.count(),method+" "+path);
        covered.add(method+" "+template);
        String response=result.getResponse().getContentAsString();
        return response.isBlank()?json.createObjectNode():json.readTree(response);
    }
    com.fasterxml.jackson.databind.JsonNode create(String resource,Object body)throws Exception {
        return write("POST","/catalog/{resource}","/catalog/"+resource,body);
    }
    Map<String,Object> promotion(String code) {
        var p=new LinkedHashMap<String,Object>();
        p.put("code",code);
        p.put("discountType","PERCENTAGE");
        p.put("discountValue",10);
        p.put("minOrderAmount",0);
        p.put("startDate",LocalDateTime.now(clock).minusDays(1).withNano(0).toString());
        p.put("endDate",LocalDateTime.now(clock).plusDays(10).withNano(0).toString());
        p.put("maxUses",100);
        p.put("maxUsesPerCustomer",1);
        p.put("active",true);
        return p;
    }
    Map<String,Object> productBody() {
        return new LinkedHashMap<>(Map.of("name","Thức ăn mới","sku",UUID.randomUUID().toString(),"price",100,"categoryId",product.getCategory().getId(),"active",true));
    }
    void rejects(String method,String path,Object body,int status)throws Exception {
        mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1/admin"+path).with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().is(status));
    }
    @Test void roleMatrixAndCompleteEndpointInventory()throws Exception {
        Set<String> expected=new HashSet<>(READS);
        expected.addAll(WRITES);
        Set<String> actual=new HashSet<>();
        mappings.getHandlerMethods().forEach((mapping,handler)-> {
            if(!Set.of(com.petcarehub.controller.AdminCatalogController.class,com.petcarehub.controller.AdminCatalogExtrasController.class).contains(handler.getBeanType()))return;assertNotNull(handler.getMethodAnnotation(org.springframework.security.access.prepost.PreAuthorize.class));mapping.getPatternValues().forEach(p->mapping.getMethodsCondition().getMethods().forEach(m->actual.add(m.name()+" "+p.substring("/api/v1/admin".length()))));
        }
        );
        assertEquals(expected,actual);
        for(var actor:List.of(admin,sales,vet,customer,warehouse))for(String endpoint:expected) {
            String[] parts=endpoint.split(" ",2);
            String path=parts[1].replace("{resource}","products").replace("{kind}","hours").replace("{id}","987654321").replace("{userId}",customer.getId().toString());
            boolean allowed=actor==admin;
            if(actor==sales)allowed=!path.startsWith("/loyalty")&&!path.startsWith("/staff-performance")&&!path.startsWith("/pet-support")&&!path.startsWith("/schedule");
            if(actor==warehouse)allowed=parts[0].equals("GET")&&path.startsWith("/catalog/products");
            if(actor==vet)allowed=path.startsWith("/pet-support");
            var response=mvc.perform(request(org.springframework.http.HttpMethod.valueOf(parts[0]),"/api/v1/admin"+path).with(as(actor)).contentType("application/json").content("{").param("from",LocalDate.now(clock).toString()).param("to",LocalDate.now(clock).toString()).param("date",appointment.getAppointmentDate().toString()).param("group","BIRTHDAY")).andReturn().getResponse();
            if(allowed)assertTrue(Set.of(200,400,404).contains(response.getStatus()),endpoint+" "+actor.getId()+" "+response.getContentAsString());
            else assertEquals(403,response.getStatus(),endpoint);
        }
        for(String resource:List.of("products","categories","species","breeds","pets","services","promotions","packages"))for(var actor:List.of(admin,sales,vet,customer,warehouse)) {
            boolean read=actor==admin||actor==sales||actor==warehouse&&Set.of("products","categories").contains(resource);
            mvc.perform(get("/api/v1/admin/catalog/"+resource).with(as(actor))).andExpect(status().is(read?200:403));
            mvc.perform(post("/api/v1/admin/catalog/"+resource).with(as(actor)).contentType("application/json").content("{")).andExpect(status().is(actor==admin||actor==sales?400:403));
        }
        mvc.perform(get("/api/v1/admin/pet-support/"+pet.getId()).with(as(otherVet))).andExpect(status().isNotFound());
        long before=audit.count();
        mvc.perform(get("/api/v1/admin/pet-support/"+pet.getId()).with(as(vet))).andExpect(status().isOk());
        assertEquals(before+1,audit.count());
    }
    @Test void everyWriteHasExactlyOneAudit()throws Exception {
        var category=create("categories",Map.of("name","Temporary"));
        long id=category.get("id").asLong();
        write("PUT","/catalog/{resource}/{id}","/catalog/categories/"+id,Map.of("name","Changed","displayOrder",2));
        write("DELETE","/catalog/{resource}/{id}","/catalog/categories/"+id,Map.of("reason","Remove unused"));
        var bulk=new LinkedHashMap<String,Object>(Map.of("ids",List.of(product.getId()),"action","PRICE_PERCENT","value",10));
        var preview=write("POST","/catalog/products/bulk-preview","/catalog/products/bulk-preview",bulk);
        bulk.put("previewToken",preview.get("previewToken").asText());
        write("POST","/catalog/products/bulk-apply","/catalog/products/bulk-apply",bulk);
        assertEquals(0,new BigDecimal("11").compareTo(product.getPrice()));
        var csv=new LinkedHashMap<String,Object>(Map.of("csv","sku,name,price,categoryId,imageUrl,description\nnew-csv,New product,12,"+product.getCategory().getId()+",,Safe"));
        preview=write("POST","/catalog/products/csv-preview","/catalog/products/csv-preview",csv);
        assertTrue(preview.get("valid").asBoolean());
        csv.put("previewToken",preview.get("previewToken").asText());
        write("POST","/catalog/products/csv-apply","/catalog/products/csv-apply",csv);
        pet.setOwner(null);
        pet.setAvailabilityStatus(AvailabilityStatus.PENDING);
        pet.setReservedOrder(order);
        order.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(pet).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());
        em.flush();
        write("POST","/catalog/pets/{id}/release-reservation","/catalog/pets/"+pet.getId()+"/release-reservation",Map.of("reason","Cancel mixed order"));
        assertEquals(OrderStatus.CANCELLED,order.getStatus());
        assertNull(pet.getReservedOrder());
        assertEquals(6,stock.getQuantity());
        pet.setOwner(customer);
        var hours=Map.of("weekday",1,"startTime","09:00","endTime","17:00","reason","Regular hours");
        id=write("POST","/schedules/{kind}","/schedules/hours",hours).get("id").asLong();
        write("PUT","/schedules/{kind}/{id}","/schedules/hours/"+id,hours);
        write("DELETE","/schedules/{kind}/{id}","/schedules/hours/"+id,Map.of("reason","Use defaults"));
        write("POST","/schedule-cancel","/schedule-cancel",Map.of("date",appointment.getAppointmentDate().toString(),"ids",List.of(appointment.getId()),"reason","Emergency closure"));
        assertEquals(AppointmentStatus.CANCELLED,appointment.getStatus());
        write("PUT","/loyalty-rules","/loyalty-rules",Map.of("earnEvery",20000,"pointValue",200,"silver",2000,"gold",6000,"platinum",12000));
        write("POST","/loyalty/{userId}/adjust","/loyalty/"+customer.getId()+"/adjust",Map.of("points",100,"reason","Customer care"));
        var pack=save(CarePackage.builder().name("Care").price(BigDecimal.TEN).durationMonths(1).build());
        var sub=save(CarePackageSubscription.builder().customer(customer).pet(pet).carePackage(pack).startDate(LocalDate.now(clock)).endDate(LocalDate.now(clock).plusMonths(1)).build());
        write("POST","/subscriptions/{id}","/subscriptions/"+sub.getId(),Map.of("action","extend","endDate",sub.getEndDate().plusMonths(1).toString(),"reason","Courtesy"));
        pet.setDateOfBirth(LocalDate.now(clock).minusYears(1));
        em.flush();
        preview=json.readTree(mvc.perform(get("/api/v1/admin/campaign-audience?group=BIRTHDAY").with(as(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var campaign=Map.of("group","BIRTHDAY","days",90,"requestKey",UUID.randomUUID().toString(),"audienceToken",preview.get("audienceToken").asText(),"message","Chúc mừng sinh nhật","promotion",promotion("BIRTHDAY"));
        write("POST","/campaigns","/campaigns",campaign);
        long notifications=em.createQuery("select count(n) from Notification n",Long.class).getSingleResult();
        write("POST","/campaigns","/campaigns",campaign);
        assertEquals(notifications,em.createQuery("select count(n) from Notification n",Long.class).getSingleResult());
        assertEquals(WRITES,covered);
    }
    @Test void crudAndInputBoundaries()throws Exception {
        var body=productBody();
        long id=create("products",body).get("id").asLong();
        assertEquals(0,em.createQuery("from Inventory where product.id=:id",Inventory.class).setParameter("id",id).getSingleResult().getQuantity());
        body.put("price",-1);
        rejects("POST","/catalog/products",body,400);
        body.put("price",5);
        body.put("categoryId",987654321);
        rejects("POST","/catalog/products",body,404);
        rejects("DELETE","/catalog/products/"+product.getId(),Map.of("reason","Referenced"),409);
        rejects("PUT","/catalog/categories/"+product.getCategory().getId(),Map.of("name","Cycle","parentId",product.getCategory().getId()),400);
        long species=create("species",Map.of("name","New species")).get("id").asLong();
        long breed=create("breeds",Map.of("name","New breed","speciesId",species)).get("id").asLong();
        var animal=new LinkedHashMap<String,Object>(Map.of("name","Bé mèo","speciesId",species,"breedId",breed,"price",500,"birthDate",LocalDate.now(clock).minusYears(1).toString(),"healthStatus","HEALTHY","availabilityStatus","AVAILABLE"));
        long petId=create("pets",animal).get("id").asLong();
        animal.put("birthDate",LocalDate.now(clock).plusDays(1).toString());
        rejects("POST","/catalog/pets",animal,400);
        animal.put("birthDate",LocalDate.now(clock).toString());
        animal.put("speciesId",pet.getSpecies().getId());
        rejects("POST","/catalog/pets",animal,400);
        write("DELETE","/catalog/{resource}/{id}","/catalog/pets/"+petId,Map.of("reason","Unused"));
        create("services",Map.of("name","Wash","price",0,"durationMinutes",30,"serviceType","GROOMING","active",true));
        create("packages",Map.of("name","Care","price",100,"durationMonths",2));
        var promo=promotion("CODEQA");
        id=create("promotions",promo).get("id").asLong();
        promo.put("code","codeqa");
        rejects("POST","/catalog/promotions",promo,409);
        var saved=em.find(Promotion.class,id);
        saved.setCurrentUses(1);
        em.flush();
        promo.put("active",false);
        write("PUT","/catalog/{resource}/{id}","/catalog/promotions/"+id,promo);
        promo.put("discountValue",20);
        rejects("PUT","/catalog/promotions/"+id,promo,409);
    }
    @Test void csvBulkAndLoyaltyRejectionsPreserveState()throws Exception {
        rejects("POST","/catalog/products/bulk-preview",Map.of("ids",Collections.nCopies(101,product.getId()),"action","ACTIVE","active",false),400);
        var body=new LinkedHashMap<String,Object>(Map.of("ids",List.of(product.getId()),"action","PRICE_AMOUNT","value",5));
        var preview=write("POST","/catalog/products/bulk-preview","/catalog/products/bulk-preview",body);
        body.put("previewToken",preview.get("previewToken").asText());
        product.setPrice(BigDecimal.valueOf(20));
        em.flush();
        rejects("POST","/catalog/products/bulk-apply",body,409);
        assertEquals(0,BigDecimal.valueOf(20).compareTo(product.getPrice()));
        var csv=new LinkedHashMap<String,Object>(Map.of("csv","sku,name,price,categoryId,imageUrl,description\nok,Valid,10,"+product.getCategory().getId()+",,Safe\nbad,=FORMULA,10,"+product.getCategory().getId()+",,Bad"));
        preview=write("POST","/catalog/products/csv-preview","/catalog/products/csv-preview",csv);
        assertFalse(preview.get("valid").asBoolean());
        csv.put("previewToken",preview.get("previewToken").asText());
        rejects("POST","/catalog/products/csv-apply",csv,400);
        assertEquals(0L,em.createQuery("select count(p) from Product p where p.sku='ok'",Long.class).getSingleResult());
        rejects("POST","/loyalty/"+customer.getId()+"/adjust",Map.of("points",-1,"reason","Insufficient"),400);
        rejects("POST","/loyalty/"+customer.getId()+"/adjust",Map.of("points",0,"reason","Zero"),400);
        write("PUT","/loyalty-rules","/loyalty-rules",Map.of("earnEvery",20000,"pointValue",200,"silver",2000,"gold",6000,"platinum",12000));
        mvc.perform(get("/api/v1/loyalty/account").with(as(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.earnEvery").value(20000)).andExpect(jsonPath("$.pointValue").value(200));
    }
    @Test void scheduleChangesImmediatelyAffectAvailabilityAndPreserveExistingAppointments()throws Exception {
        LocalDate date=appointment.getAppointmentDate();
        assertTrue(businessSchedule.available(vet.getId(),date,LocalTime.of(10,0),LocalTime.of(10,30)));
        write("POST","/schedules/{kind}","/schedules/shifts",Map.of("weekday",date.getDayOfWeek().getValue(),"staffId",vet.getId(),"startTime","11:00","endTime","16:00","reason","Shift"));
        assertFalse(businessSchedule.available(vet.getId(),date,LocalTime.of(10,0),LocalTime.of(10,30)));
        assertTrue(businessSchedule.available(vet.getId(),date,LocalTime.of(11,0),LocalTime.of(11,30)));
        write("POST","/schedules/{kind}","/schedules/time-off",Map.of("workDate",date.toString(),"staffId",vet.getId(),"startTime","11:00","endTime","12:00","reason","Leave"));
        assertFalse(businessSchedule.available(vet.getId(),date,LocalTime.of(11,0),LocalTime.of(11,30)));
        write("POST","/schedules/{kind}","/schedules/holidays",Map.of("workDate",date.toString(),"reason","Closure"));
        assertTrue(businessSchedule.hours(date).closed());
        assertEquals(AppointmentStatus.SCHEDULED,appointment.getStatus());
        mvc.perform(get("/api/v1/appointments/slots").param("date",date.toString()).param("serviceId",appointment.getService().getId().toString()).param("petId",pet.getId().toString()).with(as(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/admin/schedule-affected?date="+date).with(as(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }
    @Test void campaignCodeIsRestrictedAtCheckoutAndRetryDoesNotSendAgain()throws Exception {
        pet.setDateOfBirth(LocalDate.now(clock).minusYears(2));
        em.flush();
        var preview=json.readTree(mvc.perform(get("/api/v1/admin/campaign-audience?group=BIRTHDAY").with(as(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var campaign=new LinkedHashMap<String,Object>(Map.of("group","BIRTHDAY","days",90,"requestKey",UUID.randomUUID().toString(),"audienceToken",preview.get("audienceToken").asText(),"message","Birthday offer","promotion",promotion("PRIVATEQA")));
        write("POST","/campaigns","/campaigns",campaign);
        var body=Map.of("shipping",ShippingFixture.valid(),"paymentMethod","COD","promotionCode","privateqa","items",List.of(Map.of("productId",product.getId(),"quantity",1)));
        mvc.perform(post("/api/v1/orders/quote").with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isOk());
        var outsider=account(RoleType.CUSTOMER);
        mvc.perform(post("/api/v1/orders/quote").with(as(outsider)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());
        campaign.put("message","Different retry");
        rejects("POST","/campaigns",campaign,409);
        campaign.put("requestKey",UUID.randomUUID().toString());
        rejects("POST","/campaigns",campaign,409);
    }
    @Test void changingLoyaltyRulesDoesNotRepriceEarnedPointsForExistingOrder()throws Exception {
        product.setPrice(BigDecimal.valueOf(20000));
        em.flush();
        var rules=new LinkedHashMap<String,Object>(Map.of("earnEvery",10000,"pointValue",100,"silver",2,"gold",10,"platinum",20));
        write("PUT","/loyalty-rules","/loyalty-rules",rules);
        var body=Map.of("shipping",ShippingFixture.valid(),"paymentMethod","COD","items",List.of(Map.of("productId",product.getId(),"quantity",1)));
        var response=mvc.perform(post("/api/v1/orders").header("Idempotency-Key",UUID.randomUUID().toString()).with(as(customer)).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isOk()).andReturn().getResponse();
        long id=json.readTree(response.getContentAsByteArray()).get("id").asLong();
        assertEquals(0,BigDecimal.valueOf(10000).compareTo(em.find(Order.class,id).getRewardEarnEvery()));
        rules.put("earnEvery",20000);
        write("PUT","/loyalty-rules","/loyalty-rules",rules);
        for(String action:List.of("confirm","process","ship","deliver"))mvc.perform(post("/api/v1/admin/orders/"+id+"/transition").with(as(admin)).contentType("application/json").content(json.writeValueAsBytes(Map.of("action",action)))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/loyalty/account").with(as(customer))).andExpect(status().isOk()).andExpect(jsonPath("$.currentPoints").value(2)).andExpect(jsonPath("$.tier").value("SILVER")).andExpect(jsonPath("$.earnEvery").value(20000));
    }
}
