package com.petcarehub.config;

/** Disabled is the default; never include credential values in this operator hint. */
@org.springframework.stereotype.Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="app.bootstrap.enabled", havingValue="false", matchIfMissing=true)
@lombok.extern.slf4j.Slf4j
public class AdminBootstrapNotice implements org.springframework.boot.CommandLineRunner {
    @Override public void run(String... args) {
        log.info("Không tạo admin: chỉ bật BOOTSTRAP_ADMIN_ENABLED và cấp đủ cấu hình BOOTSTRAP_ADMIN_* khi cần khởi tạo quản trị viên");
    }
}
