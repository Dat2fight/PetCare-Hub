package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.exception.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')")
public class AdminScheduleService  {
    private final EntityManager em;
    private final AdminQuery query;
    private final AdminAuditService audit;
    private final CatalogMutex mutex;
    private final Clock clock;
    private final AppointmentService appointments;
    private static final Map<String,Class<? extends ScheduleEntry>> TYPES=Map.of("hours",BusinessHours.class,"holidays",Holiday.class,"shifts",StaffShift.class,"time-off",StaffTimeOff.class);
    private Class<? extends ScheduleEntry> type(String kind) {
        var t=TYPES.get(kind);
        if(t==null)throw new ResourceNotFoundException("Không tìm thấy lịch làm việc");
        return t;
    }
    @Transactional(readOnly=true) public Page<Map<String,Object>> list(String kind,Long staff,LocalDate from,LocalDate to,Pageable page) {
        AdminQuery.dates(from,to);
        String where="1=1";
        Map<String,Object> params=new HashMap<>();
        if(staff!=null) {
            where+=" and o.staffId=:staff";
            params.put("staff",staff);
        }
        if(Set.of("holidays","time-off").contains(kind)) {
            if(from!=null) {
                where+=" and o.workDate>=:from";
                params.put("from",from);
            }
            if(to!=null) {
                where+=" and o.workDate<=:to";
                params.put("to",to);
            }
        }
        return query.page(type(kind).getSimpleName()+" o","o.id,o.weekday,o.workDate,o.staffId,o.startTime,o.endTime,o.closed,o.reason","id,weekday,workDate,staffId,startTime,endTime,closed,reason",where,params,page,Map.of("id","o.id","workDate","o.workDate","weekday","o.weekday"));
    }
    private ScheduleEntry draft(String kind,JsonNode body) {
        var in=new CatalogInput(body);
        in.keys("weekday,workDate,staffId,startTime,endTime,closed,reason");
        ScheduleEntry row=switch(kind) {
            case "hours"->new BusinessHours();
            case "holidays"->new Holiday();
            case "shifts"->new StaffShift();
            case "time-off"->new StaffTimeOff();
            default->throw CatalogInput.bad("Loại lịch không hợp lệ");
        }
        ;
        row.setReason(in.text("reason",500,true));
        row.setClosed(kind.equals("holidays")||in.bool("closed",false));
        if(Set.of("hours","shifts").contains(kind)) {
            row.setWeekday(in.integer("weekday",1,7,0));
            if(row.getWeekday()==0)throw CatalogInput.bad("Thứ bắt buộc");
        }
        else {
            row.setWorkDate(in.date("workDate",true));
            if(row.getWorkDate().isBefore(LocalDate.now(clock)))throw CatalogInput.bad("Không sửa lịch quá khứ");
        }
        if(Set.of("shifts","time-off").contains(kind)) {
            row.setStaffId(in.id("staffId",true));
            var u=em.find(User.class,row.getStaffId());
            if(u==null||!u.getEnabled()||u.getDeletedAt()!=null||u.getRoles().stream().noneMatch(r->Set.of(RoleType.SALES_STAFF,RoleType.VETERINARIAN).contains(r.getName())))throw CatalogInput.bad("Nhân viên không đủ điều kiện");
        }
        try {
            row.setStartTime(row.isClosed()?LocalTime.MIN:LocalTime.parse(in.text("startTime",8,true)));
            row.setEndTime(row.isClosed()?LocalTime.MAX.withNano(0):LocalTime.parse(in.text("endTime",8,true)));
        }
        catch(Exception e) {
            throw CatalogInput.bad("Giờ không hợp lệ");
        }
        if(!row.getStartTime().isBefore(row.getEndTime()))throw CatalogInput.bad("Giờ bắt đầu phải trước giờ kết thúc");
        return row;
    }
    public Map<String,Object> save(String kind,Long id,JsonNode body) {
        mutex.write();
        var d=draft(kind,body);
        var cls=type(kind);
        if(kind.equals("hours")&&em.createQuery("select count(h) from BusinessHours h where h.weekday=:day and (:id is null or h.id<>:id)",Long.class).setParameter("day",d.getWeekday()).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("Đã có giờ cho thứ này, hãy sửa dòng hiện có");
        if(kind.equals("holidays")&&em.createQuery("select count(h) from Holiday h where h.workDate=:date and (:id is null or h.id<>:id)",Long.class).setParameter("date",d.getWorkDate()).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("Ngày đã đóng cửa");
        if(id==null)em.persist(d);
        else {
            var target=em.find(cls,id);
            if(target==null)throw new ResourceNotFoundException("Không tìm thấy lịch");
            target.setWeekday(d.getWeekday());
            target.setWorkDate(d.getWorkDate());
            target.setStaffId(d.getStaffId());
            target.setStartTime(d.getStartTime());
            target.setEndTime(d.getEndTime());
            target.setClosed(d.isClosed());
            target.setReason(d.getReason());
            d=target;
        }
        em.flush();
        audit.record("SCHEDULE_SAVED","SCHEDULE",d.getId(),Map.of("reasonProvided",true));
        return Map.of("id",d.getId());
    }
    public void delete(String kind,Long id,String reason) {
        if(reason==null||reason.isBlank())throw CatalogInput.bad("Cần lý do");
        mutex.write();
        var row=em.find(type(kind),id);
        if(row==null)throw new ResourceNotFoundException("Không tìm thấy lịch");
        em.remove(row);
        audit.record("SCHEDULE_DELETED","SCHEDULE",id,Map.of("reasonProvided",true));
    }
    @Transactional(readOnly=true) public Page<Map<String,Object>> affected(LocalDate date,Long staff,Pageable page) {
        if(date==null)throw CatalogInput.bad("Cần chọn ngày");
        String where="o.appointmentDate=:date and o.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED,com.petcarehub.enums.AppointmentStatus.CONFIRMED,com.petcarehub.enums.AppointmentStatus.IN_PROGRESS)";
        Map<String,Object> p=new HashMap<>(Map.of("date",date));
        if(staff!=null) {
            where+=" and s.id=:staff";
            p.put("staff",staff);
        }
        return query.page("Appointment o left join o.staff s join o.pet p join o.service v","o.id,o.appointmentDate,o.startTime,o.endTime,p.name,v.name,s.id,o.status","id,date,start,end,pet,service,staffId,status",where,p,page,Map.of("id","o.id"));
    }
    public void cancelAffected(LocalDate date,List<Long> ids,String reason) {
        if(date==null||ids==null||ids.isEmpty()||ids.size()>100||ids.stream().anyMatch(id->id==null||id<1)||new HashSet<>(ids).size()!=ids.size()||reason==null||reason.isBlank()||reason.length()>500)throw CatalogInput.bad("Chọn tối đa 100 lịch và nhập lý do");
        mutex.write();
        for(Long id:ids.stream().sorted().toList()) {
            var a=em.find(Appointment.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(a==null||!a.getAppointmentDate().equals(date))throw new ResourceNotFoundException("Lịch không thuộc ngày đã chọn");
            var req=new com.petcarehub.dto.appointment.AppointmentUpdateRequest();
            req.setStatus(AppointmentStatus.CANCELLED);
            req.setNotes(reason);
            appointments.updateAppointmentStatus(id,req);
            em.persist(Notification.builder().user(a.getCustomer()).type(NotificationType.APPOINTMENT_REMINDER).title("Lịch hẹn đã hủy").message("Cửa hàng đã hủy lịch #"+id+". Vui lòng chọn thời gian mới.").build());
        }
        audit.record("SCHEDULE_APPOINTMENTS_CANCELLED","SCHEDULE",null,Map.of("count",ids.size(),"ids",ids.toString(),"reasonProvided",true));
    }
    @PreAuthorize("@perm.has('REPORT_VIEW')") @Transactional(readOnly=true)
    public Page<Map<String,Object>> performance(LocalDate from,LocalDate to,Pageable page) {
        AdminQuery.dates(from,to);
        if(from==null||to==null||java.time.temporal.ChronoUnit.DAYS.between(from,to)>366)throw CatalogInput.bad("Kỳ tối đa 366 ngày");
        int size=Math.min(page.getPageSize(),100);
        if((long)page.getPageNumber()*size>Integer.MAX_VALUE)throw CatalogInput.bad("Trang vượt giới hạn");
        var rows=em.createQuery("select s.id,s.firstName,count(a),sum(case when a.status=com.petcarehub.enums.AppointmentStatus.COMPLETED then 1 else 0 end),sum(case when a.status=com.petcarehub.enums.AppointmentStatus.CANCELLED then 1 else 0 end),sum(case when a.status=com.petcarehub.enums.AppointmentStatus.NO_SHOW then 1 else 0 end) from Appointment a join a.staff s where a.appointmentDate>=:from and a.appointmentDate<=:to group by s.id,s.firstName order by s.id",Object[].class).setParameter("from",from).setParameter("to",to).setFirstResult(Math.toIntExact((long)page.getPageNumber()*size)).setMaxResults(size).getResultList().stream().map(v->Map.<String,Object>of("id",v[0],"name",v[1],"total",v[2],"completed",v[3],"cancelled",v[4],"noShow",v[5])).toList();
        long total=em.createQuery("select count(distinct a.staff.id) from Appointment a where a.appointmentDate>=:from and a.appointmentDate<=:to",Long.class).setParameter("from",from).setParameter("to",to).getSingleResult();
        return new PageImpl<>(rows,PageRequest.of(page.getPageNumber(),size),total);
    }
}
