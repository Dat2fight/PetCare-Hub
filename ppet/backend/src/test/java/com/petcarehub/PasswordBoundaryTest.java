package com.petcarehub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.dto.auth.*;
import com.petcarehub.entity.User;
import com.petcarehub.repository.UserRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:password-boundary;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class PasswordBoundaryTest {
    @Autowired PasswordEncoder encoder; @Autowired Validator validator; @Autowired UserRepository users;
    @Autowired MockMvc mvc; @Autowired ObjectMapper json;
    @Test void bcryptRejectsSuffixBeyond72Bytes() {
        String password="x".repeat(72);String hash=encoder.encode(password);
        assertTrue(encoder.matches(password,hash));assertFalse(encoder.matches(password+"suffix",hash));
        assertThrows(IllegalArgumentException.class,()->encoder.encode(password+"x"));
        String unicode="\u20ac".repeat(24);String unicodeHash=encoder.encode(unicode);
        assertTrue(encoder.matches(unicode,unicodeHash));assertFalse(encoder.matches(unicode+"x",unicodeHash));
    }
    @Test void registerAndLoginShareUtf8ByteLimitWithoutWeakeningMinimum() {
        var registration=new RegisterRequest();var login=new LoginRequest();
        for(String password:new String[]{"x".repeat(72),"\u20ac".repeat(24)}) {
            registration.setPassword(password);login.setPassword(password);
            assertTrue(validator.validateProperty(registration,"password").isEmpty());
            assertTrue(validator.validateProperty(login,"password").isEmpty());
        }
        for(String password:new String[]{"x".repeat(73),"\u20ac".repeat(25)," ",""}) {
            registration.setPassword(password);login.setPassword(password);
            assertFalse(validator.validateProperty(registration,"password").isEmpty());
            assertFalse(validator.validateProperty(login,"password").isEmpty());
        }
        registration.setPassword("short");assertFalse(validator.validateProperty(registration,"password").isEmpty());
    }
    @Test void httpLoginAcceptsBoundaryAndRejectsLongSuffixAndRegistration() throws Exception {
        String name=UUID.randomUUID().toString(),password="x".repeat(72);
        users.save(User.builder().username(name).email(name+"@example.test").passwordHash(encoder.encode(password)).firstName("A").lastName("B").build());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username",name,"password",password)))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username",name,"password",password+"suffix")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content(json.writeValueAsString(Map.of("username",name+"new","email","new@example.test","firstName","A","lastName","B","password","\u20ac".repeat(25)))))
                .andExpect(status().isBadRequest());
    }
}
