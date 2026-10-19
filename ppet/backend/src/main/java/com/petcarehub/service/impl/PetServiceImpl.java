package com.petcarehub.service.impl;

import com.petcarehub.dto.pet.PetCreateRequest;
import com.petcarehub.dto.pet.PetDTO;
import com.petcarehub.dto.pet.PetUpdateRequest;
import com.petcarehub.entity.Breed;
import com.petcarehub.entity.Pet;
import com.petcarehub.entity.Species;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.repository.BreedRepository;
import com.petcarehub.repository.PetRepository;
import com.petcarehub.repository.SpeciesRepository;
import com.petcarehub.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetServiceImpl implements PetService {

    private final com.petcarehub.service.PetHealthService petHealthService;
    private final PetRepository petRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final com.petcarehub.repository.UserRepository userRepository;
    private final com.petcarehub.security.Ownership ownership;
    private final com.petcarehub.config.PersonalPetProperties personalPetProperties;

    @Override
    @Transactional
    public PetDTO createMyPet(com.petcarehub.dto.pet.QuickPetCreateRequest request) {
        Long ownerId = ownership.userId();
        if (!ownership.hasRole("CUSTOMER") || java.util.stream.Stream.of("ADMIN", "SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN").anyMatch(ownership::hasRole))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ khách hàng được thêm thú cưng cá nhân");
        if (request.getName() == null || request.getName().isBlank() || request.getName().length() > 100)
            throw new com.petcarehub.exception.BadRequestException("name: Tên thú cưng phải có từ 1 đến 100 ký tự");
        if (request.getAge() == null || request.getAge() < 0 || request.getAge() > 30)
            throw new com.petcarehub.exception.BadRequestException("age: Tuổi phải từ 0 đến 30 năm");
        if (request.getSpeciesId() == null) throw new com.petcarehub.exception.BadRequestException("speciesId: Vui lòng chọn loài");
        Species species = speciesRepository.findById(request.getSpeciesId())
                .orElseThrow(() -> new com.petcarehub.exception.BadRequestException("speciesId: Loài không hợp lệ"));
        // Existing DB seeds use Dog/Cat or Chó (Dog)/Mèo (Cat). IDs are never fixed.
        if (!java.util.Set.of("dog", "cat", "chó", "mèo", "chó (dog)", "mèo (cat)").contains(species.getName().strip().toLowerCase(java.util.Locale.ROOT)))
            throw new com.petcarehub.exception.BadRequestException("speciesId: Chỉ hỗ trợ Chó hoặc Mèo");
        // Lock the account before counting, so simultaneous requests cannot exceed the cap.
        var owner = userRepository.findLockedById(ownerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (petRepository.countByOwnerId(ownerId) >= personalPetProperties.getMaxPersonalPets())
            throw new IllegalStateException("Bạn đã đạt giới hạn " + personalPetProperties.getMaxPersonalPets() + " thú cưng cá nhân");
        Pet pet = Pet.builder().name(request.getName().strip()).species(species).owner(owner)
                .dateOfBirth(java.time.LocalDate.now().minusYears(request.getAge()))
                .price(java.math.BigDecimal.ZERO).healthStatus(com.petcarehub.enums.HealthStatus.HEALTHY)
                .availabilityStatus(com.petcarehub.enums.AvailabilityStatus.UNAVAILABLE).build();
        return mapToDTO(petRepository.save(pet));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PetDTO> getPets(String healthStatus, String availabilityStatus, Long speciesId, Long breedId, Pageable pageable) {
        Specification<Pet> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("owner")));
            predicates.add(cb.equal(root.get("availabilityStatus"), com.petcarehub.enums.AvailabilityStatus.AVAILABLE));
            if (healthStatus != null && !healthStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("healthStatus"), healthStatus));
            }
            if (availabilityStatus != null && !availabilityStatus.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("availabilityStatus"), availabilityStatus));
            }
            if (speciesId != null) {
                predicates.add(cb.equal(root.get("species").get("id"), speciesId));
            }
            if (breedId != null) {
                predicates.add(cb.equal(root.get("breed").get("id"), breedId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return petRepository.findAll(spec, pageable).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public PetDTO getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));
        return mapToDTO(pet);
    }

    @Override
    @Transactional
    public PetDTO createPet(PetCreateRequest request) {
        Species species = speciesRepository.findById(request.getSpeciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species not found with id: " + request.getSpeciesId()));
        
        Breed breed = null;
        if (request.getBreedId() != null) {
            breed = breedRepository.findById(request.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed not found with id: " + request.getBreedId()));
        }

        Pet pet = new Pet();
        pet.setName(request.getName());
        pet.setSpecies(species);
        if (breed != null && !breed.getSpecies().getId().equals(species.getId())) throw new com.petcarehub.exception.BadRequestException("Giống không thuộc loài đã chọn");
        pet.setBreed(breed);
        // NEEDS DECISION: legacy age is whole years; estimate birth date only when absent.
        if (request.getBirthDate() != null) pet.setDateOfBirth(request.getBirthDate());
        else if (request.getAge() != null && pet.getDateOfBirth() == null) pet.setDateOfBirth(java.time.LocalDate.now().minusYears(request.getAge()));
        pet.setHealthStatus(parseEnum(com.petcarehub.enums.HealthStatus.class, request.getHealthStatus()));
        pet.setAvailabilityStatus(parseEnum(com.petcarehub.enums.AvailabilityStatus.class, request.getAvailabilityStatus()));
        pet.setDescription(request.getDescription());
        pet.setImageUrl(request.getImageUrl());
        pet.setPrice(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO);
        if (request.getOwnerId() != null) pet.setOwner(userRepository.findById(request.getOwnerId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ thú cưng")));
        pet.setCreatedAt(LocalDateTime.now());
        pet.setUpdatedAt(LocalDateTime.now());

        Pet savedPet = petRepository.save(pet);
        return mapToDTO(savedPet);
    }

    @Override
    @Transactional
    public PetDTO updatePet(Long id, PetUpdateRequest request) {
        Pet pet = petRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));
        if (pet.getReservedOrder() != null || pet.getAvailabilityStatus() == com.petcarehub.enums.AvailabilityStatus.PENDING)
            throw new IllegalStateException("Thú cưng đang được giữ chỗ bởi đơn hàng");

        Species species = speciesRepository.findById(request.getSpeciesId())
                .orElseThrow(() -> new ResourceNotFoundException("Species not found with id: " + request.getSpeciesId()));

        Breed breed = null;
        if (request.getBreedId() != null) {
            breed = breedRepository.findById(request.getBreedId())
                    .orElseThrow(() -> new ResourceNotFoundException("Breed not found with id: " + request.getBreedId()));
        }

        pet.setName(request.getName());
        pet.setSpecies(species);
        if (breed != null && !breed.getSpecies().getId().equals(species.getId())) throw new com.petcarehub.exception.BadRequestException("Giống không thuộc loài đã chọn");
        pet.setBreed(breed);
        // NEEDS DECISION: legacy age is whole years; estimate birth date only when absent.
        if (request.getBirthDate() != null) pet.setDateOfBirth(request.getBirthDate());
        else if (request.getAge() != null && pet.getDateOfBirth() == null) pet.setDateOfBirth(java.time.LocalDate.now().minusYears(request.getAge()));
        var newHealth = parseEnum(com.petcarehub.enums.HealthStatus.class, request.getHealthStatus());
        if (newHealth != pet.getHealthStatus()) petHealthService.updatePetHealthStatus(id, newHealth);
        pet.setAvailabilityStatus(parseEnum(com.petcarehub.enums.AvailabilityStatus.class, request.getAvailabilityStatus()));
        pet.setDescription(request.getDescription());
        pet.setImageUrl(request.getImageUrl());
        if (request.getPrice() != null) pet.setPrice(request.getPrice());
        if (request.getOwnerId() != null) pet.setOwner(userRepository.findById(request.getOwnerId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ thú cưng")));
        pet.setUpdatedAt(LocalDateTime.now());

        Pet updatedPet = petRepository.save(pet);
        return mapToDTO(updatedPet);
    }

    @Override
    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found with id: " + id));
        if (pet.getReservedOrder() != null || pet.getAvailabilityStatus() == com.petcarehub.enums.AvailabilityStatus.PENDING)
            throw new IllegalStateException("Thú cưng đang được giữ chỗ bởi đơn hàng");
        petRepository.delete(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PetDTO> getPetsByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId).stream()
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException | NullPointerException ex) { throw new com.petcarehub.exception.BadRequestException("Trạng thái không hợp lệ"); }
    }

    private PetDTO mapToDTO(Pet pet) { return com.petcarehub.mapper.PetMapper.toDTO(pet); }
}
