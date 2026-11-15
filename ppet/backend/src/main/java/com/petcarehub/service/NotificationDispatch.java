package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.NotificationType;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service @RequiredArgsConstructor
public class NotificationDispatch {
    private final EntityManager em;
    private final Clock clock;
    @Transactional(readOnly=true) public Long next(){
        var ids=em.createQuery("select b.id from NotificationBatch b where b.status='QUEUED' order by b.id",Long.class).setMaxResults(1).getResultList();
        return ids.isEmpty()?null:ids.get(0);
    }
    @Transactional public int process(Long id){
        var batch=em.find(NotificationBatch.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(batch==null)return 0;
        em.refresh(batch);
        if(!batch.getStatus().equals("QUEUED"))return 0;
        var rows=em.createQuery("select d,u from NotificationDelivery d left join User u on u.id=d.userId where d.batchId=:batch and d.status='PENDING' order by d.id",Object[].class).setParameter("batch",id).setMaxResults(100).getResultList();
        int processed=0;
        for(var pair:rows){
            var row=(NotificationDelivery)pair[0];
            var user=(User)pair[1];
            if(user==null||!Boolean.TRUE.equals(user.getEnabled())||user.getDeletedAt()!=null){
                row.setStatus("SKIPPED");
                batch.setSkipped(batch.getSkipped()+1);
                continue;
            }
            em.persist(Notification.builder().user(user).type(NotificationType.SYSTEM).title(row.getTitle()).message(row.getBody()).build());
            row.setStatus("SENT");
            batch.setSent(batch.getSent()+1);
            processed++;
        }
        if(batch.getSent()+batch.getSkipped()>=batch.getTotal()){
            batch.setStatus("COMPLETED");
            batch.setFinishedAt(LocalDateTime.now(clock));
        }
        em.flush();
        return processed;
    }
    @Transactional public void failed(Long id){
        var b=em.find(NotificationBatch.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(b==null||!b.getStatus().equals("QUEUED"))return;
        b.setAttempts(b.getAttempts()+1);
        b.setErrorCode("DELIVERY_RETRY_REQUIRED");
        if(b.getAttempts()>=5){
            b.setStatus("FAILED");
            b.setFinishedAt(LocalDateTime.now(clock));
            b.setErrorCode("DELIVERY_FAILED");
        }
    }
}
