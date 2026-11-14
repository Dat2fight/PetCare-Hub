package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
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
@Service @RequiredArgsConstructor @Transactional
public class AdminMarketingService  {
    private final EntityManager em;
    private final AdminQuery query;
    private final AdminAuditService audit;
    private final CatalogMutex mutex;
    private final Clock clock;
    private final Ownership ownership;
    private final LoyaltyPolicy policy;
    private final UserRepository users;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyTransactionRepository transactions;
    private final AdminCatalogService catalog;
    @PreAuthorize("@perm.has('SYSTEM_MANAGE')") @Transactional(readOnly=true) public LoyaltyRules rules() {
        return policy.current();
    }
    @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void rules(JsonNode body) {
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("earnEvery,pointValue,silver,gold,platinum");
        var r=policy.current();
        r.setEarnEvery(in.money("earnEvery",true));
        r.setPointValue(in.money("pointValue",true));
        int silver=in.integer("silver",1,1000000000,0),gold=in.integer("gold",1,1000000000,0),platinum=in.integer("platinum",1,1000000000,0);
        if(silver<1||silver>=gold||gold>=platinum)throw CatalogInput.bad("Ngưỡng hạng phải tăng dần");
        r.setSilver(silver);
        r.setGold(gold);
        r.setPlatinum(platinum);
        em.merge(r);
        audit.record("LOYALTY_RULES_UPDATED","LOYALTY",1L,Map.of());
    }
    @PreAuthorize("@perm.has('SYSTEM_MANAGE')") public void adjust(Long userId,JsonNode body) {
        var in=new CatalogInput(body);
        in.keys("points,reason");
        int points=in.integer("points",-1000000,1000000,0);
        if(points==0)throw CatalogInput.bad("Điểm điều chỉnh phải khác 0");
        String reason=in.text("reason",500,true);
        var user=users.findLockedById(userId).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy khách"));
        if(user.getRoles().stream().noneMatch(r->r.getName()==RoleType.CUSTOMER))throw CatalogInput.bad("Chỉ điều chỉnh điểm khách hàng");
        var a=accounts.findByCustomerId(userId).orElseGet(()->accounts.save(LoyaltyAccount.builder().customer(user).build()));
        long balance=(long)a.getAvailablePoints()+points;
        if(balance<0||balance>Integer.MAX_VALUE)throw CatalogInput.bad("Điểm không đủ hoặc vượt giới hạn");
        a.setAvailablePoints((int)balance);
        transactions.save(LoyaltyTransaction.builder().loyaltyAccount(a).transactionType(LoyaltyTransactionType.ADJUST).points(points).sourceType(LoyaltySourceType.MANUAL_ADJUSTMENT).description(reason).build());
        audit.record("LOYALTY_ADJUSTED","USER",userId,Map.of("quantity",points,"reasonProvided",true));
    }
    @PreAuthorize("@perm.has('PROMOTION_MANAGE')") @Transactional(readOnly=true) public Page<Map<String,Object>> usages(Long id,Pageable page) {
        return query.page("PromotionUsage o join o.order r","o.id,r.id,r.orderNumber,o.customer.id,o.createdAt","id,orderId,orderNumber,customerId,createdAt","o.promotion.id=:id",Map.of("id",id),page,Map.of("id","o.id"));
    }
    @PreAuthorize("@perm.has('PACKAGE_MANAGE')") @Transactional(readOnly=true) public Page<Map<String,Object>> subscriptions(Long packageId,Pageable page) {
        return query.page("CarePackageSubscription o join o.carePackage p join o.pet pet left join o.orderItem item","o.id,p.id,coalesce(item.packageName,p.name),o.customer.id,pet.name,o.startDate,o.endDate,o.status","id,packageId,name,customerId,pet,startDate,endDate,status",packageId==null?"1=1":"p.id=:id",packageId==null?Map.of():Map.of("id",packageId),page,Map.of("id","o.id")).map(row->{
            if(row.get("status")==SubscriptionStatus.ACTIVE&&((LocalDate)row.get("endDate")).isBefore(LocalDate.now(clock)))row.put("status",SubscriptionStatus.EXPIRED);
            return row;
        });
    }
    @PreAuthorize("@perm.has('PACKAGE_MANAGE')") public void subscription(Long id,JsonNode body) {
        var in=new CatalogInput(body);
        in.keys("action,endDate,reason");
        String action=in.text("action",20,true);
        in.text("reason",500,true);
        var s=em.find(CarePackageSubscription.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(s==null)throw new ResourceNotFoundException("Không tìm thấy đăng ký");
        if(action.equals("cancel")) {
            s.setStatus(SubscriptionStatus.CANCELLED);
        }
        else if(action.equals("extend")) {
            LocalDate until=in.date("endDate",true);
            if(s.getStatus()==SubscriptionStatus.CANCELLED||!until.isAfter(s.getEndDate())||until.isBefore(LocalDate.now(clock))||until.isAfter(LocalDate.now(clock).plusYears(10)))throw CatalogInput.bad("Ngày gia hạn không hợp lệ");
            s.setEndDate(until);
            s.setStatus(SubscriptionStatus.ACTIVE);
        }
        else throw CatalogInput.bad("Thao tác không hợp lệ");
        audit.record("SUBSCRIPTION_UPDATED","SUBSCRIPTION",id,Map.of("reasonProvided",true));
    }
    private List<Long> audience(String group,int days) {
        String where="u.enabled=true and u.deletedAt is null and exists(select r.id from u.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER)";
        if(group.equals("BIRTHDAY"))where+=" and exists(select p.id from Pet p where p.owner=u and month(p.dateOfBirth)=:month)";
        else if(group.equals("INACTIVE")) {
            if(days<1||days>3650)throw CatalogInput.bad("Số ngày từ 1 đến 3650");
            where+=" and u.createdAt<:cutoff and not exists(select o.id from Order o where o.customer=u and o.createdAt>=:cutoff)";
        }
        else throw CatalogInput.bad("Nhóm khách không hợp lệ");
        var q=em.createQuery("select u.id from User u where "+where+" order by u.id",Long.class);
        if(group.equals("BIRTHDAY"))q.setParameter("month",LocalDate.now(clock).getMonthValue());
        else q.setParameter("cutoff",LocalDateTime.now(clock).minusDays(days));
        return q.setMaxResults(501).getResultList();
    }
    @PreAuthorize("@perm.has('PROMOTION_MANAGE')") @Transactional(readOnly=true) public Map<String,Object> audiencePreview(String group,int days) {
        var ids=audience(group,days);
        return Map.of("count",ids.size(),"limit",500,"overLimit",ids.size()>500,"audienceToken",CatalogInput.fingerprint(ids.toString()));
    }
    @PreAuthorize("@perm.has('PROMOTION_MANAGE')") public Map<String,Object> campaign(JsonNode body) {
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("group,days,requestKey,audienceToken,promotion,message");
        String key=in.text("requestKey",36,true);
        try {
            UUID.fromString(key);
        }
        catch(Exception e) {
            throw CatalogInput.bad("Mã yêu cầu phải là UUID");
        }
        String hash=CatalogInput.fingerprint(body.toString());
        var prior=em.createQuery("from PromotionCampaign c where c.requestKey=:key",PromotionCampaign.class).setParameter("key",key).getResultList();
        if(!prior.isEmpty()) {
            var p=prior.get(0);
            if(!p.getActorId().equals(ownership.userId())||!p.getFingerprint().equals(hash))throw new IllegalStateException("Mã yêu cầu đã dùng với nội dung khác");
            audit.record("CAMPAIGN_SENT","PROMOTION",p.getPromotionId(),Map.of("replay",true));
            return Map.of("promotionId",p.getPromotionId(),"count",p.getRecipientCount());
        }
        if(em.createQuery("select count(c) from PromotionCampaign c where c.actorId=:actor and c.createdAt>:since",Long.class).setParameter("actor",ownership.userId()).setParameter("since",LocalDateTime.now(clock).minusMinutes(10)).getSingleResult()>0)throw new IllegalStateException("Mỗi nhân viên gửi tối đa một chiến dịch trong 10 phút");
        var ids=audience(in.text("group",20,true),in.integer("days",1,3650,90));
        if(ids.isEmpty()||ids.size()>500)throw CatalogInput.bad("Chiến dịch cần từ 1 đến 500 khách");
        if(!CatalogInput.fingerprint(ids.toString()).equals(in.text("audienceToken",64,true)))throw new IllegalStateException("Nhóm khách thay đổi, hãy xem trước lại");
        String message=in.text("message",500,true);
        Long promotion=catalog.saveInternal("promotions",null,new CatalogInput(body.get("promotion")));
        var p=em.find(Promotion.class,promotion);
        p.setRestrictedAudience(true);
        for(Long user:ids) {
            var a=new PromotionAudience();
            a.setPromotionId(promotion);
            a.setUserId(user);
            em.persist(a);
            em.persist(Notification.builder().user(em.getReference(User.class,user)).type(NotificationType.SYSTEM).title("Ưu đãi dành cho bạn").message(message+" · Mã: "+p.getCode()).build());
        }
        var c=new PromotionCampaign();
        c.setRequestKey(key);
        c.setActorId(ownership.userId());
        c.setPromotionId(promotion);
        c.setCreatedAt(LocalDateTime.now(clock));
        c.setRecipientCount(ids.size());
        c.setFingerprint(hash);
        em.persist(c);
        audit.record("CAMPAIGN_SENT","PROMOTION",promotion,Map.of("count",ids.size()));
        return Map.of("promotionId",promotion,"count",ids.size());
    }
}
