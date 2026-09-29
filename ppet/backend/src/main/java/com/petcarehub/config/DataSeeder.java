package com.petcarehub.config;

import com.petcarehub.entity.*;
import com.petcarehub.enums.RoleType;
import com.petcarehub.enums.ServiceType;
import com.petcarehub.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ServiceEntityRepository serviceEntityRepository;
    private final CarePackageRepository carePackageRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Database already seeded. Skipping.");
            return;
        }
        log.info("Seeding database...");
        seedRoles();
        seedAdminUser();
        seedSpeciesAndBreeds();
        seedProductCategories();
        seedServices();
        seedCarePackages();
        log.info("Database seeding complete.");
    }

    private void seedRoles() {
        Arrays.stream(RoleType.values()).forEach(roleType -> {
            Role role = Role.builder()
                    .name(roleType)
                    .description(roleType.name().replace("_", " "))
                    .build();
            roleRepository.save(role);
        });
        log.info("Roles seeded: {}", RoleType.values().length);
    }

    private void seedAdminUser() {
        Role adminRole = roleRepository.findByName(RoleType.ADMIN).orElseThrow();
        User admin = User.builder()
                .username("admin")
                .email("admin@petcarehub.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("System")
                .lastName("Administrator")
                .phone("0000000000")
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);
        log.info("Admin user seeded.");
    }

    private void seedSpeciesAndBreeds() {
        // Dogs
        Species dogs = speciesRepository.save(Species.builder().name("Dog").description("Domestic dogs").build());
        breedRepository.save(Breed.builder().species(dogs).name("Golden Retriever").sizeCategory("Large").energyLevel("High").groomingNeeds("High").goodWithChildren(true).noiseLevel("Medium").avgLifespan("10-12 years").build());
        breedRepository.save(Breed.builder().species(dogs).name("Labrador Retriever").sizeCategory("Large").energyLevel("High").groomingNeeds("Medium").goodWithChildren(true).noiseLevel("Medium").avgLifespan("10-14 years").build());
        breedRepository.save(Breed.builder().species(dogs).name("French Bulldog").sizeCategory("Small").energyLevel("Low").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("10-12 years").build());
        breedRepository.save(Breed.builder().species(dogs).name("German Shepherd").sizeCategory("Large").energyLevel("High").groomingNeeds("High").goodWithChildren(true).noiseLevel("High").avgLifespan("9-13 years").build());
        breedRepository.save(Breed.builder().species(dogs).name("Poodle").sizeCategory("Medium").energyLevel("Medium").groomingNeeds("High").goodWithChildren(true).noiseLevel("Medium").avgLifespan("12-15 years").build());
        breedRepository.save(Breed.builder().species(dogs).name("Chihuahua").sizeCategory("Small").energyLevel("Medium").groomingNeeds("Low").goodWithChildren(false).noiseLevel("High").avgLifespan("14-16 years").build());

        // Cats
        Species cats = speciesRepository.save(Species.builder().name("Cat").description("Domestic cats").build());
        breedRepository.save(Breed.builder().species(cats).name("Persian").sizeCategory("Medium").energyLevel("Low").groomingNeeds("High").goodWithChildren(true).noiseLevel("Low").avgLifespan("12-17 years").build());
        breedRepository.save(Breed.builder().species(cats).name("Siamese").sizeCategory("Medium").energyLevel("High").groomingNeeds("Low").goodWithChildren(true).noiseLevel("High").avgLifespan("15-20 years").build());
        breedRepository.save(Breed.builder().species(cats).name("Maine Coon").sizeCategory("Large").energyLevel("Medium").groomingNeeds("High").goodWithChildren(true).noiseLevel("Medium").avgLifespan("12-15 years").build());
        breedRepository.save(Breed.builder().species(cats).name("British Shorthair").sizeCategory("Medium").energyLevel("Low").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("12-20 years").build());

        // Birds
        Species birds = speciesRepository.save(Species.builder().name("Bird").description("Pet birds").build());
        breedRepository.save(Breed.builder().species(birds).name("Budgerigar").sizeCategory("Small").energyLevel("High").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Medium").avgLifespan("5-8 years").build());
        breedRepository.save(Breed.builder().species(birds).name("Cockatiel").sizeCategory("Small").energyLevel("Medium").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Medium").avgLifespan("10-14 years").build());

        // Fish
        Species fish = speciesRepository.save(Species.builder().name("Fish").description("Aquarium fish").build());
        breedRepository.save(Breed.builder().species(fish).name("Goldfish").sizeCategory("Small").energyLevel("Low").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("10-15 years").build());
        breedRepository.save(Breed.builder().species(fish).name("Betta").sizeCategory("Small").energyLevel("Low").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("3-5 years").build());

        // Reptile
        Species reptiles = speciesRepository.save(Species.builder().name("Reptile").description("Pet reptiles").build());
        breedRepository.save(Breed.builder().species(reptiles).name("Leopard Gecko").sizeCategory("Small").energyLevel("Low").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("15-20 years").build());

        // Small Animal
        Species smallAnimals = speciesRepository.save(Species.builder().name("Small Animal").description("Small furry pets").build());
        breedRepository.save(Breed.builder().species(smallAnimals).name("Hamster").sizeCategory("Small").energyLevel("Medium").groomingNeeds("Low").goodWithChildren(true).noiseLevel("Low").avgLifespan("2-3 years").build());
        breedRepository.save(Breed.builder().species(smallAnimals).name("Guinea Pig").sizeCategory("Small").energyLevel("Medium").groomingNeeds("Medium").goodWithChildren(true).noiseLevel("Medium").avgLifespan("5-7 years").build());

        log.info("Species and breeds seeded.");
    }

    private void seedProductCategories() {
        productCategoryRepository.save(ProductCategory.builder().name("Food").description("Pet food and treats").build());
        productCategoryRepository.save(ProductCategory.builder().name("Toys").description("Pet toys and entertainment").build());
        productCategoryRepository.save(ProductCategory.builder().name("Accessories").description("Collars, leashes, beds").build());
        productCategoryRepository.save(ProductCategory.builder().name("Health").description("Health supplements and care").build());
        productCategoryRepository.save(ProductCategory.builder().name("Grooming").description("Grooming tools and products").build());
        productCategoryRepository.save(ProductCategory.builder().name("Aquarium").description("Aquarium equipment and supplies").build());
        log.info("Product categories seeded.");
    }

    private void seedServices() {
        serviceEntityRepository.save(ServiceEntity.builder().name("Basic Grooming").description("Bath, brush, and nail trim").serviceType(ServiceType.GROOMING).durationMinutes(60).price(new BigDecimal("30.00")).build());
        serviceEntityRepository.save(ServiceEntity.builder().name("Full Grooming").description("Full grooming package including haircut, bath, nails, ears").serviceType(ServiceType.GROOMING).durationMinutes(120).price(new BigDecimal("60.00")).build());
        serviceEntityRepository.save(ServiceEntity.builder().name("Teeth Cleaning").description("Professional dental cleaning").serviceType(ServiceType.GROOMING).durationMinutes(30).price(new BigDecimal("25.00")).build());
        serviceEntityRepository.save(ServiceEntity.builder().name("General Checkup").description("Comprehensive health examination").serviceType(ServiceType.VETERINARY).durationMinutes(30).price(new BigDecimal("50.00")).build());
        serviceEntityRepository.save(ServiceEntity.builder().name("Vaccination").description("Standard vaccination service").serviceType(ServiceType.VETERINARY).durationMinutes(30).price(new BigDecimal("40.00")).build());
        serviceEntityRepository.save(ServiceEntity.builder().name("Surgery Consultation").description("Pre-surgery consultation and planning").serviceType(ServiceType.VETERINARY).durationMinutes(60).price(new BigDecimal("80.00")).build());
        log.info("Services seeded.");
    }

    private void seedCarePackages() {
        carePackageRepository.save(CarePackage.builder().name("Basic Care").description("Monthly basic care package").price(new BigDecimal("29.99")).durationMonths(1).includesCheckup(true).build());
        carePackageRepository.save(CarePackage.builder().name("Standard Care").description("Monthly standard care with grooming").price(new BigDecimal("49.99")).durationMonths(1).includesCheckup(true).includesGrooming(true).build());
        carePackageRepository.save(CarePackage.builder().name("Premium Care").description("Monthly premium care with everything").price(new BigDecimal("79.99")).durationMonths(1).includesCheckup(true).includesGrooming(true).includesVaccination(true).build());
        log.info("Care packages seeded.");
    }
}
