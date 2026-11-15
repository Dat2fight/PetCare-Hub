package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('SYSTEM_MANAGE')")
public class AdminNotificationService {
    private final EntityManager em;
    private final AdminQuery query;
    private final AdminAuditService audit;
    private final CatalogMutex mutex;
    private final LoyaltyPolicy loyalty;
    private final Clock clock;
    private final Ownership ownership;
    public record Audience(String group,Long userId,String role,String tier,int days){
    }
    private List<Object[]> recipients(Audience a){
        String where="u.enabled=true and u.deletedAt is null";
        Map<String,Object> params=new HashMap<>();
        switch(a.group()==null?"":a.group()){
            case "SINGLE":if(a.userId()==null||a.userId()<1)throw CatalogInput.bad("Cần chọn người nhận");
            where+=" and u.id=:user";
            params.put("user",a.userId());
            break;
            case "ROLE":RoleType role;
            try{
                role=RoleType.valueOf(a.role());
            }
            catch(Exception e){
                throw CatalogInput.bad("Vai trò không hợp lệ");
            }
            where+=" and exists(select r.id from u.roles r where r.name=:role)";
            params.put("role",role);
            break;
            case "TIER":var thresholds=loyalty.thresholds();
            if(!thresholds.containsKey(a.tier()==null?"":a.tier()))throw CatalogInput.bad("Hạng không hợp lệ");
            var levels=List.of("BRONZE","SILVER","GOLD","PLATINUM");
            int at=levels.indexOf(a.tier());
            where+=" and exists(select r.id from u.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER) and coalesce(l.totalPoints,0)>=:minimum";
            params.put("minimum",thresholds.get(a.tier()));
            if(at<3){
                where+=" and coalesce(l.totalPoints,0)<:maximum";
                params.put("maximum",thresholds.get(levels.get(at+1)));
            }
            break;
            case "INACTIVE":if(a.days()<1||a.days()>3650)throw CatalogInput.bad("Số ngày từ 1 đến 3650");
            where+=" and exists(select r.id from u.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER) and u.createdAt<:cutoff and not exists(select o.id from Order o where o.customer=u and o.createdAt>=:cutoff)";
            params.put("cutoff",LocalDateTime.now(clock).minusDays(a.days()));
            break;
            default:throw CatalogInput.bad("Nhóm người nhận không hợp lệ");
        }
        var q=em.createQuery("select u.id,u.firstName from User u left join LoyaltyAccount l on l.customer=u where "+where+" order by u.id",Object[].class);
        params.forEach(q::setParameter);
        return q.setMaxResults(501).getResultList();
    }
    private String token(List<Object[]> rows){
        return CatalogInput.fingerprint(rows.stream().map(r->r[0].toString()).toList().toString());
    }
    @Transactional(readOnly=true) public Map<String,Object> preview(Audience audience){
        var rows=recipients(audience);
        return Map.of("count",rows.size(),"limit",500,"overLimit",rows.size()>500,"audienceToken",token(rows));
    }
    public Map<String,Object> send(JsonNode body){
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("group,userId,role,tier,days,title,message,templateId,requestKey,audienceToken");
        String key=in.text("requestKey",36,true);
        try{
            UUID.fromString(key);
        }
        catch(Exception e){
            throw CatalogInput.bad("Mã yêu cầu phải là UUID");
        }
        String hash=CatalogInput.fingerprint(body.toString());
        var prior=em.createQuery("from NotificationBatch b where b.requestKey=:key",NotificationBatch.class).setParameter("key",key).getResultList();
        if(!prior.isEmpty()){
            var p=prior.get(0);
            if(!p.getActorId().equals(ownership.userId())||!p.getFingerprint().equals(hash))throw new IllegalStateException("Mã yêu cầu đã dùng với nội dung khác");
            audit.record("NOTIFICATION_QUEUED","NOTIFICATION_BATCH",p.getId(),Map.of("replay",true));
            return Map.of("id",p.getId(),"status",p.getStatus(),"total",p.getTotal());
        }
        if(em.createQuery("select count(b) from NotificationBatch b where b.actorId=:actor and b.createdAt>:since",Long.class).setParameter("actor",ownership.userId()).setParameter("since",LocalDateTime.now(clock).minusMinutes(1)).getSingleResult()>0)throw new IllegalStateException("Mỗi quản trị viên gửi tối đa một đợt mỗi phút");
        var audience=new Audience(in.text("group",20,true),in.id("userId",false),in.text("role",30,false),in.text("tier",20,false),in.integer("days",1,3650,90));
        var rows=recipients(audience);
        if(rows.isEmpty()||rows.size()>500)throw CatalogInput.bad("Chọn từ 1 đến 500 người nhận");
        if(!token(rows).equals(in.text("audienceToken",64,true)))throw new IllegalStateException("Danh sách đã thay đổi, cần xem trước lại");
        String title,message;
        Long templateId=in.id("templateId",false);
        if(templateId!=null){
            var t=em.find(NotificationTemplate.class,templateId);
            if(t==null||!t.isActive()||!t.getPurpose().equals("GENERAL"))throw CatalogInput.bad("Chỉ dùng mẫu thông báo chung đang bật");
            title=t.getTitle();
            message=t.getBody();
        }
        else{
            title=in.text("title",200,true);
            message=in.text("message",1500,true);
        }
        NotificationText.validate(title,"GENERAL");
        NotificationText.validate(message,"GENERAL");
        var batch=new NotificationBatch();
        batch.setActorId(ownership.userId());
        batch.setRequestKey(key);
        batch.setFingerprint(hash);
        batch.setTotal(rows.size());
        batch.setCreatedAt(LocalDateTime.now(clock));
        em.persist(batch);
        for(var r:rows){
            var d=new NotificationDelivery();
            d.setBatchId(batch.getId());
            d.setUserId((Long)r[0]);
            var values=Map.of("firstName",r[1]==null?"bạn":r[1].toString());
            d.setTitle(NotificationText.render(title,values,200));
            d.setBody(NotificationText.render(message,values,2000));
            em.persist(d);
        }
        audit.record("NOTIFICATION_QUEUED","NOTIFICATION_BATCH",batch.getId(),Map.of("count",rows.size(),"audienceGroup",audience.group()));
        return Map.of("id",batch.getId(),"status",batch.getStatus(),"total",batch.getTotal());
    }
    @Transactional(readOnly=true) public Page<Map<String,Object>> history(Pageable page){
        return query.page("NotificationBatch o","o.id,o.actorId,o.status,o.total,o.sent,o.skipped,o.attempts,o.createdAt,o.finishedAt,o.errorCode","id,actorId,status,total,sent,skipped,attempts,createdAt,finishedAt,errorCode","1=1",Map.of(),page,Map.of("id","o.id"));
    }
    @Transactional(readOnly=true) public Page<Map<String,Object>> templates(Pageable page){
        return query.page("NotificationTemplate o","o.id,o.code,o.purpose,o.title,o.body,o.active","id,code,purpose,title,body,active","1=1",Map.of(),page,Map.of("id","o.id","code","o.code"));
    }
    public Map<String,Object> template(Long id,JsonNode body){
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("code,purpose,title,body,active");
        String code=in.text("code",60,true).toUpperCase(Locale.ROOT);
        if(!code.matches("[A-Z0-9_]{2,60}"))throw CatalogInput.bad("Mã mẫu chỉ chứa chữ, số, gạch dưới");
        String purpose=in.text("purpose",20,true),title=in.text("title",200,true),message=in.text("body",1500,true);
        NotificationText.validate(title,purpose);
        NotificationText.validate(message,purpose);
        var t=id==null?new NotificationTemplate():em.find(NotificationTemplate.class,id);
        if(t==null)throw new ResourceNotFoundException("Không tìm thấy mẫu");
        if(id!=null&&NotificationText.RESERVED.contains(t.getCode())&&(!t.getCode().equals(code)||!t.getPurpose().equals(purpose)))throw CatalogInput.bad("Không đổi mã/loại mẫu hệ thống");
        if(em.createQuery("select count(t) from NotificationTemplate t where t.code=:code and (:id is null or t.id<>:id)",Long.class).setParameter("code",code).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("Mã mẫu đã có");
        t.setCode(code);
        t.setPurpose(purpose);
        t.setTitle(title);
        t.setBody(message);
        t.setActive(in.bool("active",true));
        if(id==null)em.persist(t);
        audit.record("NOTIFICATION_TEMPLATE_SAVED","NOTIFICATION_TEMPLATE",t.getId(),Map.of());
        return Map.of("id",t.getId());
    }
    public void deleteTemplate(Long id,String reason){
        if(reason==null||reason.isBlank())throw CatalogInput.bad("Cần lý do");
        mutex.write();
        var t=em.find(NotificationTemplate.class,id);
        if(t==null)throw new ResourceNotFoundException("Không tìm thấy mẫu");
        if(NotificationText.RESERVED.contains(t.getCode()))throw new IllegalStateException("Mẫu hệ thống chỉ được tắt");
        em.remove(t);
        audit.record("NOTIFICATION_TEMPLATE_DELETED","NOTIFICATION_TEMPLATE",id,Map.of("reasonProvided",true));
    }
}
