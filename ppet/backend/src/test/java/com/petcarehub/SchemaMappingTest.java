package com.petcarehub;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.junit.jupiter.api.Assertions.*;

class SchemaMappingTest {
    @Test void mapsSqlServerUnicodeMoneyAndCheckoutColumns() throws Exception {
        var registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false).build();
        try {
            var source = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var bean : scanner.findCandidateComponents("com.petcarehub.entity")) {
                source.addAnnotatedClass(Class.forName(bean.getBeanClassName()));
            }
            var metadata = source.getMetadataBuilder()
                    .applyPhysicalNamingStrategy(new CamelCaseToUnderscoresNamingStrategy()).build();
            var columns = new java.util.HashMap<String, String>();
            for (var namespace : metadata.getDatabase().getNamespaces()) {
                for (var table : namespace.getTables()) {
                    for (var column : table.getColumns()) {
                        columns.put(table.getName() + "." + column.getName(), column.getSqlType(metadata));
                    }
                }
            }
            assertAll(
                    () -> assertEquals("nvarchar(100)", columns.get("pets.name")),
                    () -> assertEquals("nvarchar(max)", columns.get("pets.description")),
                    () -> assertEquals("nvarchar(100)", columns.get("orders.recipient_name")),
                    () -> assertEquals("nvarchar(max)", columns.get("roles.description")),
                    () -> assertEquals("numeric(18,2)", columns.get("orders.total_amount")),
                    () -> assertEquals("varchar(500)", columns.get("products.image_url")),
                    () -> assertEquals("varchar(36)", columns.get("orders.checkout_key")),
                    () -> assertEquals("varchar(64)", columns.get("orders.checkout_fingerprint")));
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
