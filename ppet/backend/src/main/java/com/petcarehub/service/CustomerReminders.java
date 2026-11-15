package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.NotificationType;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class CustomerReminders {
    private final EntityManager em;
    private final CatalogMutex mutex;
    private final Clock clock;
    private NotificationTemplate template(String code){
        return em.createQuery("from NotificationTemplate t where t.code=:code and t.active=true",NotificationTemplate.class).setParameter("code",code).getResultStream().findFirst().orElse(null);
    }
    @Transactional public int run(){
        mutex.write();
        var now=LocalDateTime.now(clock);
        var until=now.plusHours(24);
        int count=0;
        var appointment=template("APPOINTMENT_REMINDER");
        if(appointment!=null){
            var rows=em.createQuery("select a.id,a.customer.id,a.customer.firstName,a.pet.name,a.appointmentDate,a.startTime from Appointment a where a.customer.enabled=true and a.customer.deletedAt is null and a.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED,com.petcarehub.enums.AppointmentStatus.CONFIRMED) and (a.appointmentDate>:today or a.appointmentDate=:today and a.startTime>=:time) and (a.appointmentDate<:untilDate or a.appointmentDate=:untilDate and a.startTime<=:untilTime) and not exists(select r.id from ReminderReceipt r where r.kind='APPOINTMENT' and r.sourceId=a.id and r.sourceDate=a.appointmentDate and r.sourceTime=a.startTime) order by a.id",Object[].class).setParameter("today",now.toLocalDate()).setParameter("time",now.toLocalTime()).setParameter("untilDate",until.toLocalDate()).setParameter("untilTime",until.toLocalTime()).setMaxResults(100).getResultList();
            for(var row:rows){
                LocalDate date=(LocalDate)row[4];
                LocalTime time=(LocalTime)row[5];
                send("APPOINTMENT",(Long)row[0],(Long)row[1],date,time,appointment,Map.of("firstName",Objects.toString(row[2],"bạn"),"petName",row[3].toString(),"appointmentTime",date+" "+time));
                count++;
            }
        }
        var vaccination=template("VACCINATION_REMINDER");
        if(vaccination!=null){
            var rows=em.createQuery("select v.id,v.pet.owner.id,v.pet.owner.firstName,v.pet.name,v.nextDueDate from Vaccination v where v.pet.owner.enabled=true and v.pet.owner.deletedAt is null and v.nextDueDate>=:today and v.nextDueDate<=:until and not exists(select r.id from ReminderReceipt r where r.kind='VACCINATION' and r.sourceId=v.id and r.sourceDate=v.nextDueDate) order by v.id",Object[].class).setParameter("today",now.toLocalDate()).setParameter("until",now.toLocalDate().plusDays(7)).setMaxResults(100).getResultList();
            for(var row:rows){
                LocalDate date=(LocalDate)row[4];
                send("VACCINATION",(Long)row[0],(Long)row[1],date,LocalTime.MIDNIGHT,vaccination,Map.of("firstName",Objects.toString(row[2],"bạn"),"petName",row[3].toString(),"dueDate",date.toString()));
                count++;
            }
        }
        return count;
    }
    private void send(String kind,Long source,Long user,LocalDate date,LocalTime time,NotificationTemplate template,Map<String,String> values){
        em.persist(Notification.builder().user(em.getReference(User.class,user)).type(NotificationType.APPOINTMENT_REMINDER).title(NotificationText.render(template.getTitle(),values,200)).message(NotificationText.render(template.getBody(),values,2000)).build());
        var receipt=new ReminderReceipt();
        receipt.setKind(kind);
        receipt.setSourceId(source);
        receipt.setSourceDate(date);
        receipt.setSourceTime(time);
        receipt.setCreatedAt(LocalDateTime.now(clock));
        em.persist(receipt);
    }
}
