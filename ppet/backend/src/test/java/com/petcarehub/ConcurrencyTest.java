package com.petcarehub;

import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.dto.appointment.AppointmentCreateRequest;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@ActiveProfiles("test")
class ConcurrencyTest {
    @Autowired UserRepository users; @Autowired RoleRepository roles; @Autowired ProductRepository products;
    @Autowired ProductCategoryRepository categories; @Autowired InventoryRepository inventory;
    @Autowired InventoryService stock; @Autowired SpeciesRepository species; @Autowired PetRepository pets;
    @Autowired ServiceEntityRepository services; @Autowired AppointmentService booking;

    private void race(User user, Runnable action) throws Exception {
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1); AtomicInteger success=new AtomicInteger();
        Callable<Void> task=()->{ start.await(); var principal=UserDetailsImpl.build(user);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            try {action.run();success.incrementAndGet();} catch(com.petcarehub.exception.BadRequestException | IllegalStateException expected) { }
            finally {SecurityContextHolder.clearContext();} return null;};
        try {Future<Void> a=pool.submit(task),b=pool.submit(task);start.countDown();a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);assertEquals(1,success.get());}
        finally {pool.shutdownNow();}
    }
    private User account(String name) {return users.save(User.builder().username(name).email(name+"@example.test").passwordHash("unused").firstName("A").lastName("B").build());}
    @Test void concurrentStockOutCannotOversell() throws Exception {
        User user=account("stock-race"); var category=categories.save(ProductCategory.builder().name("Race").build());
        var product=products.save(Product.builder().name("Race").category(category).price(BigDecimal.ONE).sku("RACE").active(true).build());
        inventory.save(Inventory.builder().product(product).quantity(1).build());
        race(user,()->stock.reduceStock(product.getId(),1));assertEquals(0,inventory.findByProductId(product.getId()).orElseThrow().getQuantity());
    }
    @Test void concurrentBookingCannotDoubleBook() throws Exception {
        User customer=account("booking-race"); Role role=roles.save(Role.builder().name(RoleType.VETERINARIAN).build());
        User vet=account("vet-race");vet.setRoles(Set.of(role));users.save(vet);
        Species animal=species.save(Species.builder().name("Race").build());
        Pet pet=pets.save(Pet.builder().name("Race").species(animal).owner(customer).price(BigDecimal.ZERO).healthStatus(HealthStatus.HEALTHY).availabilityStatus(AvailabilityStatus.UNAVAILABLE).build());
        ServiceEntity service=services.save(ServiceEntity.builder().name("Race").serviceType(ServiceType.VETERINARY).durationMinutes(60).price(BigDecimal.TEN).active(true).build());
        AppointmentCreateRequest request=new AppointmentCreateRequest();request.setPetId(pet.getId());request.setServiceId(service.getId());request.setStaffId(vet.getId());request.setAppointmentTime(LocalDate.now().plusDays(1).atTime(10,0));
        race(customer,()->booking.bookAppointment(request));
    }
}
