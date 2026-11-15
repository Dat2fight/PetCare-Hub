package com.petcarehub;
import com.petcarehub.entity.*;
import com.petcarehub.service.NotificationDispatch;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class NotificationConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired NotificationDispatch dispatch;
    @Autowired PlatformTransactionManager manager;
    @Test void committedQueueSurvivesTwoCompetingWorkersWithoutDuplicateDelivery()throws Exception{
        var tx=new TransactionTemplate(manager);
        String marker="queue_"+UUID.randomUUID();
        List<Long> users=new ArrayList<>();
        long batch=tx.execute(status->{
            for(int i=0;i<8;i++){
                String name=UUID.randomUUID().toString();var u=User.builder().username(name).email(name+"@example.test").passwordHash("unused").firstName("Worker").lastName("Test").build();em.persist(u);users.add(u.getId());
            }
            var b=new NotificationBatch();b.setActorId(users.get(0));b.setRequestKey(UUID.randomUUID().toString());b.setFingerprint("a".repeat(64));b.setTotal(users.size());b.setCreatedAt(LocalDateTime.of(2026,11,15,12,0));em.persist(b);for(Long user:users){
                var d=new NotificationDelivery();d.setBatchId(b.getId());d.setUserId(user);d.setTitle(marker);d.setBody("Durable message");em.persist(d);
            }
            return b.getId();
        }
        );
        var pool=Executors.newFixedThreadPool(2);
        var gate=new CountDownLatch(1);
        try{
            var a=pool.submit(()->{
                gate.await();return dispatch.process(batch);
            }
            );
            var b=pool.submit(()->{
                gate.await();return dispatch.process(batch);
            }
            );
            gate.countDown();
            assertEquals(8,a.get(15,TimeUnit.SECONDS)+b.get(15,TimeUnit.SECONDS));
            tx.executeWithoutResult(status->{
                assertEquals(8L,em.createQuery("select count(n) from Notification n where n.title=:marker",Long.class).setParameter("marker",marker).getSingleResult());assertEquals("COMPLETED",em.find(NotificationBatch.class,batch).getStatus());
            }
            );
            assertEquals(0,dispatch.process(batch));
        }
        finally{
            pool.shutdownNow();
            tx.executeWithoutResult(status->{
                em.createQuery("delete from Notification where title=:marker").setParameter("marker",marker).executeUpdate();em.createQuery("delete from NotificationDelivery where batchId=:id").setParameter("id",batch).executeUpdate();em.createQuery("delete from NotificationBatch where id=:id").setParameter("id",batch).executeUpdate();em.createQuery("delete from User where id in :ids").setParameter("ids",users).executeUpdate();
            }
            );
        }
    }
}
