package com.petcarehub.repository;
import com.petcarehub.entity.AdminAuditLog;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;

/** Deliberately exposes neither deletion nor updating existing entries. */
public interface AdminAuditRepository extends Repository<AdminAuditLog,Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<AdminAuditLog> {
    AdminAuditLog save(AdminAuditLog entry);
    Page<AdminAuditLog> findAll(Specification<AdminAuditLog> filter, Pageable pageable);
    long count();
}
