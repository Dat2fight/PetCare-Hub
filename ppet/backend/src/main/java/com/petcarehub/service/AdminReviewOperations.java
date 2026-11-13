package com.petcarehub.service;
import com.petcarehub.dto.admin.OperationRequests;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.service.impl.ReviewServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('REVIEW_MANAGE')")
public class AdminReviewOperations {
    private final ReviewServiceImpl reviews;private final AdminQuery query;private final AdminAuditService audit;private final jakarta.persistence.EntityManager em;
    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(String status,String type,Long target,Pageable page){
        if(!Set.of("PENDING","APPROVED","REJECTED","HIDDEN","ALL").contains(status))throw new BadRequestException("Trạng thái không hợp lệ");
        String where="1=1";Map<String,Object> p=new HashMap<>();
        if(!"ALL".equals(status)){where+=" and coalesce(o.moderationState,'PENDING')=:status";p.put("status",status);}
        if(type!=null&&!type.isBlank()){if(!Set.of("PRODUCT","PET","SERVICE").contains(type))throw new BadRequestException("Đối tượng không hợp lệ");where+=" and o.reviewableType=:type";p.put("type",type);}
        if(target!=null){where+=" and o.reviewableId=:target";p.put("target",target);}
        return query.page("Review o","o.id,o.reviewableType,o.reviewableId,o.rating,o.comment,o.moderationState,o.moderationReason,o.createdAt",
            "id,type,targetId,rating,comment,status,reason,createdAt",where,p,page,Map.of("id","o.id","createdAt","o.createdAt","rating","o.rating"));
    }
    @Transactional(readOnly=true) public Map<String,Long> count(){return Map.of("pending",em.createQuery("select count(r) from Review r where coalesce(r.moderationState,'PENDING')='PENDING'",Long.class).getSingleResult());}
    public void moderate(Long id,OperationRequests.Moderate request){if("REJECTED".equals(request.action())&&(request.reason()==null||request.reason().isBlank()))throw new BadRequestException("Cần nhập lý do từ chối");reviews.moderate(id,request.action(),request.reason());audit.record("REVIEW_"+request.action(),"REVIEW",id,Map.of("statusAfter",request.action(),"reasonProvided",request.reason()!=null));}
}
