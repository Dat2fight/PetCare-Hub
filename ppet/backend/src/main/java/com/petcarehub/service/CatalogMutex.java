package com.petcarehub.service;
import com.petcarehub.entity.AdminCatalogState;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
public class CatalogMutex  {
    private final EntityManager em;
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional public void initialize() {
        if(em.find(AdminCatalogState.class,1)==null) {
            var row=new AdminCatalogState();
            row.setId(1);
            em.persist(row);
        }
    }
    // Same stable row serializes tree mutations, codes and schedule changes with new bookings.
    public void write() {
        em.find(AdminCatalogState.class,1,LockModeType.PESSIMISTIC_WRITE);
    }
    public void read() {
        em.find(AdminCatalogState.class,1,LockModeType.PESSIMISTIC_READ);
    }
}
