package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.repository.OrderRepository;
import com.petcarehub.security.Ownership;
import com.petcarehub.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import java.time.*;
import java.math.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class ReturnsService {
    private final EntityManager em;
    private final OrderRepository orders;
    private final AdminQuery query;
    private final Ownership ownership;
    private final AdminAuditService audit;
    private final InventoryService inventory;
    private final Clock clock;
    @PreAuthorize("hasRole('CUSTOMER')") public Object create(JsonNode body){
        var in=new CatalogInput(body);
        in.keys("orderId,itemId,quantity,reason,requestKey");
        long user=ownership.userId();
        var order=orders.findOwnedLockedById(in.id("orderId",true),user).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn"));
        String key=in.text("requestKey",36,true);
        try{
            UUID.fromString(key);
        }
        catch(Exception e){
            throw CatalogInput.bad("requestKey phải là UUID");
        }
        String hash=CatalogInput.fingerprint(body.toString());
        var old=em.createQuery("from ReturnRequest r where r.customerId=:user and r.requestKey=:key",ReturnRequest.class).setParameter("user",user).setParameter("key",key).getResultList();
        if(!old.isEmpty()){
            if(!old.get(0).getFingerprint().equals(hash))throw new IllegalStateException("Mã yêu cầu đã dùng với nội dung khác");
            return Map.of("id",old.get(0).getId());
        }
        if(order.getStatus()!=OrderStatus.DELIVERED||order.getPaymentStatus()!=PaymentStatus.COMPLETED||order.getDeliveredAt()==null||LocalDateTime.now(clock).isAfter(order.getDeliveredAt().plusDays(order.getReturnWindowDays()==null?7:order.getReturnWindowDays()))||order.getRefundRequestedAt()!=null)throw new IllegalStateException("Đơn chưa đủ điều kiện, đang hoàn toàn bộ hoặc đã hết hạn trả hàng");
        Long itemId=in.id("itemId",true);
        var item=order.getItems().stream().filter(i->i.getId().equals(itemId)&&i.getItemType()==OrderItemType.PRODUCT&&i.getProduct()!=null).findFirst().orElseThrow(()->CatalogInput.bad("Chỉ trả sản phẩm trong đơn này"));
        int quantity=in.integer("quantity",1,item.getQuantity(),0);
        if(quantity<1)throw CatalogInput.bad("Cần số lượng");
        long used=em.createQuery("select coalesce(sum(r.quantity),0) from ReturnRequest r where r.itemId=:item and r.status<>'REJECTED'",Long.class).setParameter("item",itemId).getSingleResult();
        if(used+quantity>item.getQuantity())throw new IllegalStateException("Số lượng đã có yêu cầu trả hàng");
        BigDecimal products=order.getItems().stream().filter(i->i.getProduct()!=null).map(OrderItem::getSubtotal).reduce(BigDecimal.ZERO,BigDecimal::add);
        BigDecimal net=products.subtract(order.getDiscountAmount()).subtract(order.getPointsDiscount()==null?BigDecimal.ZERO:order.getPointsDiscount()).max(BigDecimal.ZERO);
        BigDecimal maximum=products.signum()==0?BigDecimal.ZERO:item.getSubtotal().multiply(net).multiply(BigDecimal.valueOf(quantity)).divide(products.multiply(BigDecimal.valueOf(item.getQuantity())),0,RoundingMode.DOWN);
        var r=new ReturnRequest();
        r.setOrderId(order.getId());
        r.setItemId(itemId);
        r.setCustomerId(user);
        r.setQuantity(quantity);
        r.setReason(in.text("reason",500,true));
        r.setRequestKey(key);
        r.setFingerprint(hash);
        r.setMaximumRefund(maximum);
        r.setCreatedAt(LocalDateTime.now(clock));
        em.persist(r);
        notice(order,"Đã nhận yêu cầu trả hàng #"+r.getId());
        return Map.of("id",r.getId());
    }
    @PreAuthorize("hasRole('CUSTOMER')") @Transactional(readOnly=true) public Object mine(Pageable p){
        return list("o.customerId=:user",Map.of("user",ownership.userId()),p);
    }
    @PreAuthorize("@perm.has('ORDER_MANAGE')") @Transactional(readOnly=true) public Object queue(String status,Pageable p){
        if(status!=null&&!Set.of("REQUESTED","APPROVED","REJECTED","RECEIVED","REFUNDED").contains(status))throw CatalogInput.bad("Trạng thái không hợp lệ");
        return list(status==null?"1=1":"o.status=:status",status==null?Map.of():Map.of("status",status),p);
    }
    private Object list(String where,Map<String,Object> params,Pageable p){
        return query.page("ReturnRequest o join OrderItem i on i.id=o.itemId left join i.product product","o.id,o.orderId,o.itemId,o.quantity,o.status,o.reason,o.decisionReason,o.maximumRefund,o.refundAmount,o.createdAt,o.updatedAt,coalesce(i.itemNameSnapshot,product.name)","id,orderId,itemId,quantity,status,reason,decisionReason,maximumRefund,refundAmount,createdAt,updatedAt,itemName",where,params,p,Map.of("id","o.id"));
    }
    @PreAuthorize("@perm.has('ORDER_MANAGE')") public void transition(long id,JsonNode body){
        var in=new CatalogInput(body);
        in.keys("action,reason,amount,reference");
        var found=em.find(ReturnRequest.class,id);
        if(found==null)throw new ResourceNotFoundException("Không tìm thấy yêu cầu");
        var order=orders.findLockedById(found.getOrderId()).orElseThrow();
        em.refresh(found);
        var r=found;
        String action=in.text("action",20,true),reason=in.text("reason",500,true);
        String before=r.getStatus();
        String next=switch(action){
            case "approve"->"APPROVED";
            case "reject"->"REJECTED";
            case "receive"->"RECEIVED";
            case "refund"->"REFUNDED";
            default->throw CatalogInput.bad("Thao tác không hợp lệ");
        }
        ;
        if(before.equals(next)){
            if(action.equals("refund")&&(!Objects.equals(r.getRefundReference(),in.text("reference",100,true))||r.getRefundAmount().compareTo(in.money("amount",false))!=0))throw new IllegalStateException("Biên nhận không khớp lần đã hoàn");
            audit.record("RETURN_"+next,"RETURN",id,Map.of("replay",true));
            return;
        }
        boolean valid=before.equals("REQUESTED")&&Set.of("approve","reject").contains(action)||before.equals("APPROVED")&&Set.of("receive","reject").contains(action)||before.equals("RECEIVED")&&action.equals("refund");
        if(!valid)throw new IllegalStateException("Chuyển trạng thái trả hàng không hợp lệ");
        if(order.getPaymentStatus()!=PaymentStatus.COMPLETED||order.getRefundRequestedAt()!=null)throw new IllegalStateException("Đơn đã hoàn tiền hoặc đang hoàn toàn bộ");
        if(action.equals("receive")){
            var item=em.find(OrderItem.class,r.getItemId());
            var stock=new com.petcarehub.dto.inventory.StockUpdateRequest();
            stock.setProductId(item.getProduct().getId());
            stock.setQuantity(r.getQuantity());
            stock.setReason("Nhận hàng trả #"+id+": "+reason);
            inventory.addStock(stock);
        }
        if(action.equals("refund")){
            BigDecimal amount=in.money("amount",false);
            String reference=in.text("reference",100,true);
            if(amount.compareTo(r.getMaximumRefund())>0||amount.compareTo(order.getTotalAmount().subtract(order.getReturnedAmount()))>0)throw CatalogInput.bad("Tiền hoàn vượt giá sản phẩm thực trả hoặc số tiền còn lại của đơn");
            r.setRefundAmount(amount);
            r.setRefundReference(reference);
            order.setReturnedAmount(order.getReturnedAmount().add(amount));
            em.persist(OrderPaymentEvent.builder().orderId(order.getId()).eventType("RETURN_REFUND_"+id).amount(amount).reference(reference).actorId(ownership.userId()).occurredAt(LocalDateTime.now(clock)).build());
        }
        r.setStatus(next);
        r.setDecisionReason(reason);
        r.setHandledBy(ownership.userId());
        r.setUpdatedAt(LocalDateTime.now(clock));
        notice(order,"Yêu cầu trả hàng #"+id+": "+next);
        audit.record("RETURN_"+next,"RETURN",id,Map.of("statusBefore",before,"statusAfter",next,"reasonProvided",true,"quantity",r.getQuantity()));
        em.flush();
    }
    private void notice(Order order,String message){
        em.persist(Notification.builder().user(order.getCustomer()).type(NotificationType.SYSTEM).title("Trả hàng sản phẩm").message(message).build());
    }
}
