package com.petcarehub.service;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @PreAuthorize("@perm.has('SYSTEM_MANAGE')")
public class AdminSystemService {
    private final DataSource datasource;
    private final ObjectProvider<Flyway> flyway;
    private final ObjectProvider<BuildProperties> build;
    private final JobMonitor monitor;
    private final EntityManager em;
    private final Clock clock;
    public Map<String,Object> status(){
        boolean connected;
        try(var connection=datasource.getConnection();var stmt=connection.createStatement()){
            stmt.setQueryTimeout(2);
            try(var rs=stmt.executeQuery("SELECT 1")){
                connected=rs.next();
            }
        }
        catch(Exception e){
            connected=false;
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("database",connected?"CONNECTED":"UNAVAILABLE");
        result.put("checkedAt",LocalDateTime.now(clock));
        var info=build.getIfAvailable();
        result.put("version",info==null?Objects.toString(getClass().getPackage().getImplementationVersion(),"development"):info.getVersion());
        result.put("paymentGateway","NOT_CONFIGURED_CASH_ONLY");
        if(!connected){
            result.put("jobs",List.of());
            result.put("migration","UNKNOWN");
            return result;
        }
        try{
            var f=flyway.getIfAvailable();
            result.put("migration",f==null?"DISABLED":f.info().current()==null?"NONE":f.info().current().getVersion().toString());
        }
        catch(RuntimeException e){
            result.put("migration","UNAVAILABLE");
        }
        result.put("jobs",monitor.latest());
        result.put("failedPaymentsLast7Days",em.createQuery("select count(o) from Order o where o.paymentStatus=com.petcarehub.enums.PaymentStatus.FAILED and o.createdAt>=:since",Long.class).setParameter("since",LocalDateTime.now(clock).minusDays(7)).getSingleResult());
        result.put("expiringOrders",em.createQuery("select count(o) from Order o where o.status=com.petcarehub.enums.OrderStatus.PENDING and o.paymentStatus=com.petcarehub.enums.PaymentStatus.PENDING and o.expiresAt>=:now and o.expiresAt<=:soon",Long.class).setParameter("now",LocalDateTime.now(clock)).setParameter("soon",LocalDateTime.now(clock).plusHours(2)).getSingleResult());
        return result;
    }
}
