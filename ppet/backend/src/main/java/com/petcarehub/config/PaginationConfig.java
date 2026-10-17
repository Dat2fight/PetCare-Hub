package com.petcarehub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class PaginationConfig {
    @Bean
    PageableHandlerMethodArgumentResolverCustomizer boundedPages() {
        return resolver -> { resolver.setMaxPageSize(100); resolver.setFallbackPageable(org.springframework.data.domain.PageRequest.of(0, 20)); };
    }
}
