package com.petcarehub.config;

import com.petcarehub.security.JwtAuthenticationFilter;
import com.petcarehub.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private com.petcarehub.security.SecurityErrorHandler securityErrors;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder() {
            @Override public boolean matches(CharSequence rawPassword, String encodedPassword) {
                // Upstream permits legacy long-password verification. Our API policy is strict.
                // Run BCrypt first so DAO dummy-hash timing mitigation is retained.
                boolean matches = super.matches(rawPassword, encodedPassword);
                return matches && rawPassword.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors
                    .authenticationEntryPoint((request, response, exception) -> securityErrors.unauthorized(request, response))
                    .accessDeniedHandler((request, response, exception) -> securityErrors.forbidden(request, response)))
                .authorizeHttpRequests(auth -> {
                    // Reject forbidden writes before JSON binding/validation; controller guards remain in place.
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/products/manage").hasAnyRole("ADMIN", "SALES_STAFF", "WAREHOUSE_STAFF");
                    for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE}) {
                        auth.requestMatchers(method, "/api/v1/products/**", "/api/v1/categories/**", "/api/v1/promotions/**", "/api/v1/services/**")
                            .hasAnyRole("ADMIN", "SALES_STAFF");
                        auth.requestMatchers(method, "/api/v1/medical-records/**", "/api/v1/vaccinations/**", "/api/v1/treatments/**")
                            .hasAnyRole("ADMIN", "VETERINARIAN");
                        auth.requestMatchers(method, "/api/v1/inventory/**").hasAnyRole("ADMIN", "WAREHOUSE_STAFF");
                    }
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/pets/my-pets").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pets").hasAnyRole("ADMIN", "SALES_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/pets/{id}").hasAnyRole("ADMIN", "SALES_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/pets/{id}").hasAnyRole("ADMIN", "SALES_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reviews/{id}/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/loyalty/account/{userId}/add").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/loyalty/account/spend", "/api/v1/loyalty/account/{userId}/spend").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders/{id}/confirm", "/api/v1/orders/{id}/process", "/api/v1/orders/{id}/ship", "/api/v1/orders/{id}/deliver")
                            .hasAnyRole("ADMIN", "SALES_STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/v1/orders").hasRole("CUSTOMER")
                        .requestMatchers("/api/v1/auth/me", "/api/v1/pets/my-pets", "/api/v1/pets/my-pets/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/pets/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/product-categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/species/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/breeds/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/care-packages/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/promotions/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated();
                });

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
