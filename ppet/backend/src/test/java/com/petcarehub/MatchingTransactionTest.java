package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.matching.QuizSubmitRequest;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.UserDetailsImpl;
import com.petcarehub.service.PetMatchingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:matching-transaction;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class MatchingTransactionTest {
    @Autowired UserRepository users; @Autowired PetRepository pets; @Autowired SpeciesRepository species;
    @Autowired PetMatchingQuizRepository quizzes; @SpyBean PetMatchingResultRepository results;
    @Autowired PetMatchingService service; @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    User customer; QuizSubmitRequest request;
    @BeforeEach void setup() {
        results.deleteAll();quizzes.deleteAll();pets.deleteAll();
        String n=UUID.randomUUID().toString();customer=users.save(User.builder().username(n).email(n+"@example.test").passwordHash("unused").firstName("A").lastName("B").build());
        var kind=species.save(Species.builder().name(n).build());
        for(int i=0;i<12;i++) pets.save(Pet.builder().name("Pet "+i).species(kind).price(BigDecimal.TEN)
                .dateOfBirth(LocalDate.now().minusYears(i%2==0?8:1)).weight(BigDecimal.TEN).description("friendly").build());
        request=new QuizSubmitRequest();request.setActivityLevel("HIGH");request.setAvailableCareTime(4);
        request.setHouseSize("SMALL");request.setLivingEnvironment("APARTMENT");request.setPresenceOfChildren(true);
    }
    @Test void persistsRanksInScoreOrderAndReturnsSameTopTen() {
        var response=service.matchPets(customer.getId(),request);
        var stored=results.findAll().stream().sorted(Comparator.comparing(PetMatchingResult::getRank)).toList();
        assertEquals(12,stored.size());assertEquals(10,response.size());assertEquals(1,quizzes.count());
        for(int i=0;i<stored.size();i++) {
            assertEquals(i+1,stored.get(i).getRank());
            if(i>0) {
                assertTrue(stored.get(i-1).getOverallScore()>=stored.get(i).getOverallScore());
                if(stored.get(i-1).getOverallScore().equals(stored.get(i).getOverallScore()))
                    assertTrue(stored.get(i-1).getPet().getId()<stored.get(i).getPet().getId());
            }
            if(i<10) assertEquals(stored.get(i).getPet().getId(),response.get(i).getPet().getId());
        }
    }
    @Test void resultFailureRollsBackQuizAndEvenAlreadyFlushedResult() {
        doAnswer(call->{
            Iterable<PetMatchingResult> batch=call.getArgument(0);
            results.saveAndFlush(batch.iterator().next());
            throw new IllegalStateException("Simulated result persistence failure");
        }).when(results).saveAll(any());
        var error=assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class,()->service.matchPets(customer.getId(),request));
        assertInstanceOf(IllegalStateException.class,error.getCause());
        assertEquals(0,quizzes.count());assertEquals(0,results.count());
    }
    @Test void legacyUrlCannotOverridePrincipalCustomer() throws Exception {
        mvc.perform(post("/api/v1/pet-matching/submit/9223372036854775807").with(user(UserDetailsImpl.build(customer)))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isForbidden());
        assertEquals(0,quizzes.count());
        mvc.perform(post("/api/v1/pet-matching/submit").with(user(UserDetailsImpl.build(customer)))
                .contentType("application/json").content(json.writeValueAsString(request)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
        assertEquals(customer.getId(),quizzes.findAll().get(0).getCustomer().getId());
    }
}
