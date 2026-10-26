package com.petcarehub;

import com.petcarehub.config.AdminBootstrap;
import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(OutputCaptureExtension.class)
class QaSecretConfigurationTest {
    @Test void runtimeDatabaseCredentialsMustComeFromEnvironmentWithoutPasswordsInFallbacks() throws Exception {
        for(String file:List.of("application.yml","application-dev.yml")) {
            String source=Files.readString(Path.of("src/main/resources",file));
            assertTrue(source.contains("password: ${DB_PASSWORD}"),file+": spring.datasource.password must have no fallback");
            assertTrue(source.contains("username: ${DB_USERNAME}"),file+": spring.datasource.username must come from environment");
            assertTrue(source.contains("url: ${DB_URL:"),file+": spring.datasource.url must be configurable");
        }
    }
    @Test void testKeyIsClearlyMarkedAndCannotBePackagedAsRuntimeProfile() throws Exception {
        String source=Files.readString(Path.of("src/test/resources/application-test.yml"));
        assertTrue(source.contains("secret: test-only-"),"Test-only JWT material must be visibly marked");
        assertFalse(Files.exists(Path.of("src/main/resources/application-test.yml")));
    }
    @Test void weakBootstrapPasswordIsRejectedBeforeWriting() {
        var users=mock(UserRepository.class);var roles=mock(RoleRepository.class);
        var env=new MockEnvironment().withProperty("app.bootstrap.username","qa_admin")
                .withProperty("app.bootstrap.email","qa_admin@example.test").withProperty("app.bootstrap.password","q".repeat(16));
        assertThrows(IllegalStateException.class,()->new AdminBootstrap(users,roles,new BCryptPasswordEncoder(),env).run());
        verifyNoInteractions(users,roles);
    }
    @Test void bootstrapStoresOnlyABcryptHashAndNeverLogsPassword(CapturedOutput output) {
        var users=mock(UserRepository.class);var roles=mock(RoleRepository.class);var encoder=new BCryptPasswordEncoder();
        String password=UUID.randomUUID()+"Aa9!";
        var env=new MockEnvironment().withProperty("app.bootstrap.username","qa_admin")
                .withProperty("app.bootstrap.email","qa_admin@example.test").withProperty("app.bootstrap.password",password);
        when(roles.findByName(any())).thenAnswer(call->Optional.of(Role.builder().name(call.getArgument(0)).build()));
        new AdminBootstrap(users,roles,encoder,env).run();
        var captured=org.mockito.ArgumentCaptor.forClass(User.class);verify(users).save(captured.capture());
        assertTrue(encoder.matches(password,captured.getValue().getPasswordHash()),"Bootstrap must hash credentials");
        assertFalse(output.getAll().contains(password),"Credentials must not be logged");
        assertFalse(output.getAll().contains(captured.getValue().getPasswordHash()),"Hashes must not be logged");
    }
}
