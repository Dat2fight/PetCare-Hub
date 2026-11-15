package com.petcarehub;
import com.petcarehub.entity.Role;
import com.petcarehub.entity.User;
import com.petcarehub.enums.RoleType;
import com.petcarehub.security.JwtTokenProvider;
import com.petcarehub.security.UserDetailsImpl;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Uses a real servlet ASYNC dispatch; MockMvc's mock principal hides JWT redispatch bugs. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminCsvHttpTest {
    @LocalServerPort int port;
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired JwtTokenProvider tokens;
    @Test void jwtCsvDownloadCompletesAndAuditsOnceAcrossAsyncDispatch() throws Exception {
        var tx = new TransactionTemplate(manager);
        User user = tx.execute(status -> {
            var role = em.createQuery("select r from Role r where r.name=:name", Role.class)
            .setParameter("name", RoleType.ADMIN).getResultStream().findFirst().orElseGet(() -> {
                var created = new Role(); created.setName(RoleType.ADMIN); em.persist(created); return created;
            }
            );
            String marker = "csv_" + UUID.randomUUID();
            var created = User.builder().username(marker).email(marker + "@example.test")
            .passwordHash("unused").firstName("CSV").lastName("Test")
            .roles(new HashSet<>(Set.of(role))).build();
            em.persist(created); return created;
        }
        );
        try {
            var principal = UserDetailsImpl.build(user);
            String token = tokens.generateToken(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            var url = URI.create("http://127.0.0.1:" + port + "/api/v1/admin/reports/export?kind=revenue&from=2026-11-01&to=2026-11-02");
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            var request = HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(10)).header("Authorization", "Bearer " + token).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertTrue(response.headers().firstValue("content-disposition").orElse("").contains("attachment"));
            String csv = new String(response.body(), StandardCharsets.UTF_8);
            assertTrue(csv.startsWith("\uFEFF"));
            assertTrue(csv.contains("Doanh thu đã thanh toán"));
            assertTrue(csv.contains("2026-11-02"));
            assertEquals(401, client.send(HttpRequest.newBuilder(url).timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.discarding()).statusCode());
            tx.executeWithoutResult(status -> assertEquals(1L, em.createQuery("select count(a) from AdminAuditLog a where a.actorUserId=:actor and a.action='CSV_EXPORT_PREPARED'", Long.class).setParameter("actor", user.getId()).getSingleResult()));
        }
        finally {
            tx.executeWithoutResult(status -> {
                em.createQuery("delete from AdminAuditLog where actorUserId=:actor").setParameter("actor", user.getId()).executeUpdate();
                em.remove(em.find(User.class, user.getId()));
            }
            );
        }
    }
}
