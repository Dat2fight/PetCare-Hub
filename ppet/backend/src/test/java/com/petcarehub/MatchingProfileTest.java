package com.petcarehub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.RoleRepository;
import com.petcarehub.security.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class MatchingProfileTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired RoleRepository roles;
    User admin, customer;
    Species species;

    <T> T persist(T entity) { em.persist(entity); em.flush(); return entity; }
    User account(RoleType type) {
        var role = roles.findByName(type).orElseGet(() -> roles.save(Role.builder().name(type).build()));
        String name = UUID.randomUUID().toString();
        return persist(User.builder().username(name).email(name+"@example.test").passwordHash("unused")
                .firstName("Test").lastName("Matching").roles(Set.of(role)).build());
    }
    @BeforeEach void setup() {
        admin = account(RoleType.ADMIN); customer = account(RoleType.CUSTOMER);
        species = persist(Species.builder().name("Matching " + UUID.randomUUID()).build());
    }
    Map<String,Object> body() {
        return new LinkedHashMap<>(Map.of("name", "Bé mới", "price", 100, "speciesId", species.getId(),
                "healthStatus", "HEALTHY", "availabilityStatus", "AVAILABLE", "activityLevel", "HIGH",
                "suitableForApartment", true, "goodWithChildren", true, "minimumDailyCareHours", 2));
    }
    Map<String,Object> answers() {
        return new LinkedHashMap<>(Map.of("livingEnvironment", "APARTMENT", "presenceOfChildren", true,
                "activityLevel", "HIGH", "availableCareTime", 2));
    }
    JsonNode save(Long id, Map<String,Object> body) throws Exception {
        return json.readTree(mvc.perform((id == null ? post("/api/v1/admin/catalog/pets")
                : put("/api/v1/admin/catalog/pets/"+id)).with(user(UserDetailsImpl.build(admin)))
                .contentType("application/json").content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    JsonNode matches(Map<String,Object> answers) throws Exception {
        return json.readTree(mvc.perform(post("/api/v1/pet-matching/submit").with(user(UserDetailsImpl.build(customer)))
                .contentType("application/json").content(json.writeValueAsBytes(answers)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
    }
    JsonNode find(JsonNode results, long id) {
        for (JsonNode result : results) if (result.path("pet").path("id").asLong() == id) return result;
        return null;
    }
    JsonNode fallback(JsonNode results, long id) {
        var result = find(results, id);
        assertNotNull(result);
        assertTrue(result.path("fallback").asBoolean());
        assertFalse(result.path("unmetCriteria").isEmpty());
        return result;
    }
    @Test void fallbackOnlyAppearsWhenNoFullyEligiblePetExistsEvenIfItsScoreIsHigher() throws Exception {
        var full = body(); full.put("activityLevel", "LOW");
        long fullId = save(null, full).path("id").asLong();
        var near = body(); near.put("minimumDailyCareHours", 2.01);
        long nearId = save(null, near).path("id").asLong();
        var response = matches(answers());
        assertEquals(1, response.size());
        assertEquals(70, find(response, fullId).path("score").asInt());
        assertFalse(find(response, fullId).path("fallback").asBoolean());
        assertTrue(find(response, fullId).path("unmetCriteria").isEmpty());
        assertNull(find(response, nearId));
        full.put("availabilityStatus", "UNAVAILABLE"); save(fullId, full);
        assertEquals(99, fallback(matches(answers()), nearId).path("score").asInt());
    }
    @Test void nearestResultsUseActualCareDeficitAndStableRanksWithTopTenLimit() throws Exception {
        var ids = new ArrayList<Long>();
        for (int i = 0; i < 12; i++) {
            var input = body(); input.put("minimumDailyCareHours", i < 2 ? 3 : i + 2);
            ids.add(save(null, input).path("id").asLong());
        }
        var response = matches(answers());
        assertEquals(10, response.size());
        for (int i = 0; i < response.size(); i++) {
            assertEquals(ids.get(i).longValue(), response.get(i).path("pet").path("id").asLong());
            fallback(response, ids.get(i));
            if (i > 0) assertTrue(response.get(i-1).path("score").asInt() >= response.get(i).path("score").asInt());
        }
        assertEquals(90, response.get(0).path("score").asInt());
        var stored = em.createQuery("from PetMatchingResult r where r.quiz.customer.id=:id order by r.rank", PetMatchingResult.class)
                .setParameter("id", customer.getId()).getResultList();
        assertEquals(12, stored.size());
        for (int i = 0; i < stored.size(); i++) {
            assertEquals(i+1, stored.get(i).getRank());
            assertEquals(ids.get(i), stored.get(i).getPet().getId());
            assertTrue(stored.get(i).getExplanation().startsWith("Gợi ý gần nhất"));
        }
    }
    @Test void zeroScoreStillReportsEveryMismatchWithoutClaimingCompatibility() throws Exception {
        var input = body(); input.put("suitableForApartment", false); input.put("goodWithChildren", false);
        long id = save(null, input).path("id").asLong();
        var answer = answers(); answer.put("activityLevel", "LOW"); answer.put("availableCareTime", 0);
        var result = fallback(matches(answer), id);
        assertEquals(0, result.path("score").asInt());
        assertEquals(4, result.path("unmetCriteria").size());
        assertTrue(result.path("explanation").asText().contains("không phù hợp căn hộ"));
        assertTrue(result.path("explanation").asText().contains("không phù hợp với trẻ nhỏ"));
    }
    @Test void newAdminPetPersistsAndUpdatesDriveNextMatchingRequest() throws Exception {
        var input = body(); input.put("minimumDailyCareHours", 1.5);
        long id = save(null, input).path("id").asLong();
        em.flush(); em.clear();
        Pet stored = em.find(Pet.class, id);
        assertEquals("HIGH", stored.getActivityLevel());
        assertEquals(0, new BigDecimal("1.5").compareTo(stored.getMinimumDailyCareHours()));
        mvc.perform(get("/api/v1/admin/catalog/pets/"+id).with(user(UserDetailsImpl.build(admin))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.goodWithChildren").value(true))
                .andExpect(jsonPath("$.minimumDailyCareHours").value(1.5));
        var match = find(matches(answers()), id);
        assertNotNull(match); assertEquals(100, match.path("score").asInt());
        assertTrue(match.path("explanation").asText().contains("1.5 giờ/ngày"));
        input.put("activityLevel", "LOW"); save(id, input);
        assertEquals(70, find(matches(answers()), id).path("score").asInt());
        input.put("minimumDailyCareHours", 3); save(id, input);
        assertEquals(60, fallback(matches(answers()), id).path("score").asInt());
        input.put("minimumDailyCareHours", 2); input.put("activityLevel", null); save(id, input);
        assertNull(find(matches(answers()), id));
    }
    @Test void missingTraitsNeverReceiveGuessedScoresEvenWithFriendlyDescriptionAndYoungAge() throws Exception {
        for (String key : List.of("activityLevel", "suitableForApartment", "goodWithChildren", "minimumDailyCareHours")) {
            var input = body(); input.remove(key); input.put("description", "friendly, thân thiện, hiền lành");
            input.put("birthDate", LocalDate.now().minusYears(1).toString());
            long id = save(null, input).path("id").asLong();
            assertNull(find(matches(answers()), id), key);
        }
    }
    @Test void housingChildrenAndCareAreIndividualEligibilityRequirements() throws Exception {
        var input = body(); input.put("suitableForApartment", false);
        long id = save(null, input).path("id").asLong();
        fallback(matches(answers()), id);
        var answer = answers(); answer.put("livingEnvironment", "HOUSE");
        assertNotNull(find(matches(answer), id));
        input.put("goodWithChildren", false); save(id, input);
        fallback(matches(answer), id);
        answer.put("presenceOfChildren", false);
        assertNotNull(find(matches(answer), id));
        answer.put("activityLevel", "LOW"); fallback(matches(answer), id);
        answer.put("activityLevel", "MEDIUM"); fallback(matches(answer), id);
        answer.put("activityLevel", "HIGH");
        answer.put("availableCareTime", 1); fallback(matches(answer), id);
        answer.put("availableCareTime", 0); fallback(matches(answer), id);
    }
    @Test void ageWeightAndMarketingCopyDoNotChangeScoresAndUnavailablePetsAreExcluded() throws Exception {
        var input = body(); long id = save(null, input).path("id").asLong();
        var before = find(matches(answers()), id);
        var pet = em.find(Pet.class, id);
        pet.setDateOfBirth(LocalDate.now().minusYears(18)); pet.setWeight(BigDecimal.valueOf(80));
        pet.setDescription("not friendly"); em.flush();
        var after = find(matches(answers()), id);
        assertEquals(before.path("score"), after.path("score"));
        assertEquals(before.path("explanation"), after.path("explanation"));
        for (var state : List.of(AvailabilityStatus.UNAVAILABLE, AvailabilityStatus.PENDING, AvailabilityStatus.SOLD)) {
            pet.setAvailabilityStatus(state); em.flush(); assertNull(find(matches(answers()), id));
        }
        pet.setAvailabilityStatus(AvailabilityStatus.AVAILABLE); pet.setOwner(customer); em.flush();
        assertNull(find(matches(answers()), id));
    }
    @Test void invalidProfileAndSurveyValuesAreRejected() throws Exception {
        for (var invalid : List.of(Map.entry("activityLevel", (Object)"GUESS"), Map.entry("minimumDailyCareHours", (Object)0),
                Map.entry("minimumDailyCareHours", (Object)(-1)), Map.entry("minimumDailyCareHours", (Object)25),
                Map.entry("minimumDailyCareHours", (Object)1.234), Map.entry("suitableForApartment", (Object)"yes"))) {
            var input = body(); input.put(invalid.getKey(), invalid.getValue());
            mvc.perform(post("/api/v1/admin/catalog/pets").with(user(UserDetailsImpl.build(admin)))
                    .contentType("application/json").content(json.writeValueAsBytes(input))).andExpect(status().isBadRequest());
        }
        for (String key : List.of("activityLevel", "livingEnvironment")) {
            var input = answers(); input.put(key, "GUESS");
            mvc.perform(post("/api/v1/pet-matching/submit").with(user(UserDetailsImpl.build(customer)))
                    .contentType("application/json").content(json.writeValueAsBytes(input))).andExpect(status().isBadRequest());
        }
    }
}
