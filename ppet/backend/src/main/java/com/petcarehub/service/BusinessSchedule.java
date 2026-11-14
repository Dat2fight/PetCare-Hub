package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.config.BookingProperties;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class BusinessSchedule  {
    private final EntityManager em;
    private final BookingProperties defaults;
    public record Window(LocalTime start,LocalTime end,boolean closed) {
    }
    public Window hours(LocalDate date) {
        if(em.createQuery("select count(h) from Holiday h where h.workDate=:date",Long.class).setParameter("date",date).getSingleResult()>0)return new Window(defaults.getOpeningTime(),defaults.getClosingTime(),true);
        var rows=em.createQuery("from BusinessHours h where h.weekday=:day",BusinessHours.class).setParameter("day",date.getDayOfWeek().getValue()).getResultList();
        if(rows.isEmpty())return new Window(defaults.getOpeningTime(),defaults.getClosingTime(),false);
        var h=rows.get(0);
        return new Window(h.getStartTime(),h.getEndTime(),h.isClosed());
    }
    public boolean available(Long staff,LocalDate date,LocalTime start,LocalTime end) {
        var h=hours(date);
        if(h.closed()||start.isBefore(h.start())||end.isAfter(h.end())||!start.isBefore(end))return false;
        if(em.createQuery("select count(t) from StaffTimeOff t where t.staffId=:staff and t.workDate=:date and t.startTime<:end and t.endTime>:start",Long.class).setParameter("staff",staff).setParameter("date",date).setParameter("start",start).setParameter("end",end).getSingleResult()>0)return false;
        var shifts=em.createQuery("from StaffShift s where s.staffId=:staff and s.weekday=:day",StaffShift.class).setParameter("staff",staff).setParameter("day",date.getDayOfWeek().getValue()).getResultList();
        // No weekly roster means the legacy business-hours schedule. A configured roster excludes unlisted days.
        long configured=em.createQuery("select count(s) from StaffShift s where s.staffId=:staff",Long.class).setParameter("staff",staff).getSingleResult();
        return configured==0||shifts.stream().anyMatch(s->!s.isClosed()&&!start.isBefore(s.getStartTime())&&!end.isAfter(s.getEndTime()));
    }
}
