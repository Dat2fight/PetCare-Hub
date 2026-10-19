package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.pet.QuickPetCreateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.*;
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
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:quick-pets;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "app.pets.max-personal-pets=2"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class QuickPetCreateTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired PetService service; @Autowired PetRepository pets;
    @Autowired UserRepository users; @Autowired SpeciesRepository species; @Autowired RoleRepository roles;
    @Autowired ServiceEntityRepository services; @Autowired AppointmentService booking;
    User customer; Species dog;
    User account() { String n=UUID.randomUUID().toString();return users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build()); }
    UserDetailsImpl caller(String role) {return new UserDetailsImpl(customer.getId(),customer.getUsername(),customer.getEmail(),"unused",List.of(new SimpleGrantedAuthority("ROLE_"+role)));}
    void authenticate() {var p=caller("CUSTOMER");SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));}
    QuickPetCreateRequest request() {var r=new QuickPetCreateRequest();r.setName("Bông");r.setSpeciesId(dog.getId());r.setAge(2);return r;}
    @BeforeEach void setup() { customer=account();dog=species.findByName("Chó (Dog)").orElseGet(()->species.save(Species.builder().name("Chó (Dog)").build()));authenticate(); }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}
    @Test void createsOnlyForPrincipalAndStaysPrivate() throws Exception {
        var body=Map.of("name","Bông","speciesId",dog.getId(),"age",2,"ownerId",999,"price",99999,"availabilityStatus","AVAILABLE");
        var result=mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(json.writeValueAsString(body)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(customer.getId())).andExpect(jsonPath("$.age").value(2))
                .andExpect(jsonPath("$.speciesName").value("Chó (Dog)")).andExpect(jsonPath("$.availabilityStatus").value("UNAVAILABLE")).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        var saved=pets.findById(id).orElseThrow();assertEquals(LocalDate.now().minusYears(2),saved.getDateOfBirth());assertNull(saved.getBreed());assertEquals(0,saved.getPrice().signum());
        mvc.perform(get("/api/v1/pets/{id}",id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/pets").param("speciesId",dog.getId().toString())).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void validatesFieldsAndSpeciesWithoutFixedIds() throws Exception {
        for(int age:new int[]{-1,31}) {var r=request();r.setAge(age);invalid(r,"age");}
        var r=request();r.setName(" ");invalid(r,"name");r.setName("x".repeat(101));invalid(r,"name");
        r=request();r.setSpeciesId(Long.MAX_VALUE);invalid(r,"speciesId");
        var bird=species.findByName("Bird").orElseGet(()->species.save(Species.builder().name("Bird").build()));r.setSpeciesId(bird.getId());invalid(r,"speciesId");
        mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json")
                .content("{\"name\":\"Bông\",\"speciesId\":"+dog.getId()+",\"age\":1.5}")).andExpect(status().isBadRequest());
        authenticate();r=request();r.setAge(-1);var invalid=r;
        assertThrows(com.petcarehub.exception.BadRequestException.class,()->service.createMyPet(invalid));assertEquals(0,pets.countByOwnerId(customer.getId()));
    }
    void invalid(QuickPetCreateRequest request,String field) throws Exception {
        mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message",containsString(field)));
    }
    @Test void anonymousAndStaffCannotCreate() throws Exception {
        String body=json.writeValueAsString(request());SecurityContextHolder.clearContext();
        mvc.perform(post("/api/v1/pets/my-pets").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        for(String role:List.of("ADMIN","SALES_STAFF","WAREHOUSE_STAFF","VETERINARIAN"))
            mvc.perform(post("/api/v1/pets/my-pets").with(user(caller(role))).contentType("application/json").content(body)).andExpect(status().isForbidden());
    }
    @Test void limitIsConfigurableAndReturns409() throws Exception {
        assertEquals(20,new com.petcarehub.config.PersonalPetProperties().getMaxPersonalPets());
        service.createMyPet(request());service.createMyPet(request());
        mvc.perform(post("/api/v1/pets/my-pets").with(user(caller("CUSTOMER"))).contentType("application/json").content(json.writeValueAsString(request())))
                .andExpect(status().isConflict());assertEquals(2,pets.countByOwnerId(customer.getId()));
    }
    @Test void concurrentCreationCannotExceedLimit() throws Exception {
        service.createMyPet(request());var pool=Executors.newFixedThreadPool(2);var ready=new CountDownLatch(2);var start=new CountDownLatch(1);
        try {
            Callable<Boolean> add=()->{authenticate();ready.countDown();try{start.await();service.createMyPet(request());return true;}catch(IllegalStateException ex){return false;}finally{SecurityContextHolder.clearContext();}};
            var a=pool.submit(add);var b=pool.submit(add);assertTrue(ready.await(5,TimeUnit.SECONDS));start.countDown();
            assertNotEquals(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertEquals(2,pets.countByOwnerId(customer.getId()));
        } finally{start.countDown();pool.shutdownNow();}
    }
    @Test void newCustomerCanBookWithCreatedPet() {
        var pet=service.createMyPet(request());
        var vet=account();vet.setRoles(Set.of(roles.findByName(RoleType.VETERINARIAN).orElseGet(()->roles.save(Role.builder().name(RoleType.VETERINARIAN).build()))));vet=users.save(vet);
        var care=services.save(ServiceEntity.builder().name("Khám").serviceType(ServiceType.VETERINARY).price(BigDecimal.TEN).durationMinutes(60).active(true).build());
        var r=new com.petcarehub.dto.appointment.AppointmentCreateRequest();r.setPetId(pet.getId());r.setServiceId(care.getId());r.setStaffId(vet.getId());r.setAppointmentTime(LocalDate.now().plusDays(1).atTime(10,0));
        var appointment=booking.bookAppointment(r);assertEquals(pet.getId(),appointment.getPetId());assertEquals(customer.getId(),appointment.getCustomerId());
    }
}
