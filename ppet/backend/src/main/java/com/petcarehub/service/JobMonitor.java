package com.petcarehub.service;
import com.petcarehub.entity.JobRun;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class JobMonitor {
    public static final List<String> NAMES=List.of("ORDER_EXPIRY","NOTIFICATION_DISPATCH","CUSTOMER_REMINDERS","DATA_EXPORT");
    private final EntityManager em;
    private final Clock clock;
    @Transactional(propagation=Propagation.REQUIRES_NEW) public Long start(String name){
        if(!NAMES.contains(name))throw new IllegalArgumentException("Unknown job");
        var r=new JobRun();
        r.setName(name);
        r.setStartedAt(LocalDateTime.now(clock));
        em.persist(r);
        return r.getId();
    }
    @Transactional(propagation=Propagation.REQUIRES_NEW) public void finish(Long id,int processed,boolean failed){
        var r=em.find(JobRun.class,id);
        r.setFinishedAt(LocalDateTime.now(clock));
        r.setProcessed(processed);
        r.setStatus(failed?"FAILED":"SUCCESS");
        r.setErrorCode(failed?"JOB_EXECUTION_FAILED":null);
        var old=em.createQuery("select j.id from JobRun j where j.name=:name and j.finishedAt is not null order by j.id desc",Long.class).setParameter("name",r.getName()).setFirstResult(100).setMaxResults(200).getResultList();
        if(!old.isEmpty())em.createQuery("delete from JobRun where id in :ids").setParameter("ids",old).executeUpdate();
    }
    @Transactional(readOnly=true) public List<Map<String,Object>> latest(){
        var rows=em.createQuery("select j.name,j.startedAt,j.finishedAt,j.processed,j.status,j.errorCode from JobRun j where not exists(select n.id from JobRun n where n.name=j.name and n.id>j.id)",Object[].class).getResultList();
        var result=new ArrayList<Map<String,Object>>();
        for(String name:NAMES){
            Map<String,Object> item=new LinkedHashMap<>();
            item.put("name",name);
            item.put("status","NEVER_RUN");
            for(var row:rows)if(name.equals(row[0])){
                item.put("startedAt",row[1]);
                item.put("finishedAt",row[2]);
                item.put("processed",row[3]);
                item.put("status",row[4]);
                item.put("errorCode",row[5]);
            }
            result.add(item);
        }
        return result;
    }
}
