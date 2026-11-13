package com.petcarehub.service;
import com.petcarehub.dto.admin.OperationRequests;
import com.petcarehub.dto.appointment.AppointmentUpdateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.*;
import com.petcarehub.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
@PreAuthorize("@perm.has('APPOINTMENT_MANAGE') or @perm.has('ASSIGNED_APPOINTMENT_MANAGE')")
public class AdminAppointmentOperations {
    private final AppointmentRepository appointments;private final UserRepository users;private final PetRepository pets;
    private final AppointmentService service;private final AdminQuery query;private final AdminAuditService audit;
    private final Ownership ownership;private final PermissionService permissions;private final NotificationRepository notifications;
    private final Clock clock;private final jakarta.persistence.EntityManager em;
    private Appointment accessible(Long id){var a=appointments.findById(id).orElseThrow(this::missing);if(!permissions.has("APPOINTMENT_MANAGE")&&(a.getStaff()==null||!a.getStaff().getId().equals(ownership.userId())))throw missing();return a;}
    private ResourceNotFoundException missing(){return new ResourceNotFoundException("Không tìm thấy lịch hẹn");}
    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(Long staff,Long serviceId,AppointmentStatus status,LocalDate from,LocalDate to,boolean unassigned,Pageable page){
        AdminQuery.dates(from,to);Map<String,Object> params=new HashMap<>();String where="1=1";
        if(!permissions.has("APPOINTMENT_MANAGE")){if(staff!=null&&!staff.equals(ownership.userId()))throw missing();staff=ownership.userId();}
        if(staff!=null){where+=" and s.id=:staff";params.put("staff",staff);}
        if(serviceId!=null){where+=" and v.id=:service";params.put("service",serviceId);}
        if(status!=null){where+=" and o.status=:status";params.put("status",status);}
        if(from!=null){where+=" and o.appointmentDate>=:from";params.put("from",from);}
        if(to!=null){where+=" and o.appointmentDate<=:to";params.put("to",to);}
        if(unassigned)where+=" and o.staff is null";
        return rows(where,params,page);
    }
    private Page<Map<String,Object>> rows(String where,Map<String,Object> params,Pageable page){return query.page("Appointment o join o.pet p join o.service v left join o.staff s",
        "o.id,p.name,v.name,o.appointmentDate,o.startTime,o.endTime,o.status,s.id,concat(concat(s.lastName,' '),s.firstName),v.id,o.notes",
        "id,pet,service,date,start,end,status,staffId,staffName,serviceId,notes",where,params,page,Map.of("id","o.id","date","o.appointmentDate","status","o.status"));}
    public Map<String,Object> detail(Long id){accessible(id);var result=rows("o.id=:id",Map.of("id",id),PageRequest.of(0,1)).getContent().get(0);audit.record("APPOINTMENT_READ","APPOINTMENT",id,Map.of());return result;}
    @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')") @Transactional(readOnly=true)
    public Page<Map<String,Object>> eligible(Long id,Pageable page){var a=accessible(id);return query.page("User o join o.roles r","o.id,concat(concat(o.lastName,' '),o.firstName)","id,name",
        "o.enabled=true and o.deletedAt is null and r.name=:role",Map.of("role",a.getService().getServiceType().staffRole()),page,Map.of("id","o.id"));}
    @PreAuthorize("@perm.has('APPOINTMENT_MANAGE')")
    public void assign(Long id,OperationRequests.Assign request){
        var snapshot=accessible(id);
        // Same resource order as customer booking: pet, candidate staff, then appointment.
        pets.findByIdForUpdate(snapshot.getPet().getId()).orElseThrow(this::missing);
        var staff=users.findLockedById(request.staffId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy nhân viên"));
        var a=appointments.findLockedById(id).orElseThrow(this::missing);em.refresh(a);
        if(!Set.of(AppointmentStatus.SCHEDULED,AppointmentStatus.CONFIRMED).contains(a.getStatus())||!LocalDateTime.of(a.getAppointmentDate(),a.getStartTime()).isAfter(LocalDateTime.now(clock)))throw new IllegalStateException("Chỉ phân công lại lịch sắp tới chưa bắt đầu");
        if(!Boolean.TRUE.equals(staff.getEnabled())||staff.getDeletedAt()!=null||staff.getRoles().stream().noneMatch(r->r.getName()==a.getService().getServiceType().staffRole()))throw new BadRequestException("Nhân viên không hoạt động hoặc không đúng vai trò dịch vụ");
        if(appointments.findConflictingAppointments(a.getAppointmentDate(),staff.getId(),a.getStartTime(),a.getEndTime()).stream().anyMatch(x->!x.getId().equals(id))||appointments.findPetConflicts(a.getAppointmentDate(),a.getPet().getId(),a.getStartTime(),a.getEndTime()).stream().anyMatch(x->!x.getId().equals(id)))throw new IllegalStateException("Nhân viên hoặc thú cưng có lịch trùng");
        a.setStaff(staff);audit.record("APPOINTMENT_ASSIGNED","APPOINTMENT",id,Map.of("staffId",staff.getId(),"reasonProvided",true));
    }
    public void status(Long id,OperationRequests.Status request){
        accessible(id);if(request.status()==AppointmentStatus.CANCELLED&&(request.reason()==null||request.reason().isBlank()))throw new BadRequestException("Cần nhập lý do hủy lịch");
        var r=new AppointmentUpdateRequest();r.setStatus(request.status());r.setNotes(request.reason());service.updateAppointmentStatus(id,r);
        if(request.status()==AppointmentStatus.CANCELLED)notifications.save(Notification.builder().user(appointments.findById(id).orElseThrow().getCustomer()).type(NotificationType.APPOINTMENT_REMINDER)
            .title("Lịch hẹn đã hủy").message("Lịch hẹn #"+id+" đã được cửa hàng hủy. Vui lòng xem chi tiết lịch hẹn.").build());
        audit.record("APPOINTMENT_STATUS_CHANGED","APPOINTMENT",id,Map.of("statusAfter",request.status().name(),"reasonProvided",request.reason()!=null));
    }
}
