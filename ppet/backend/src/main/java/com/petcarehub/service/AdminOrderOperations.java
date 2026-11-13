package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.dto.admin.OperationRequests;
import com.petcarehub.dto.order.OrderDTO;
import com.petcarehub.repository.OrderRepository;
import com.petcarehub.security.Ownership;
import com.petcarehub.exception.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('ORDER_MANAGE')")
public class AdminOrderOperations {
    private final OrderRepository orders;
    private final OrderService service;
    private final AdminQuery query;
    private final AdminAuditService audit;
    private final Ownership ownership;
    private final Clock clock;
    private final EntityManager em;
    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(OrderStatus status,PaymentStatus payment,String keyword,LocalDate from,LocalDate to,boolean refunds,Pageable page) {
        AdminQuery.dates(from,to);String where="1=1";Map<String,Object> params=new HashMap<>();
        if(status!=null){where+=" and o.status=:status";params.put("status",status);}
        if(payment!=null){where+=" and o.paymentStatus=:payment";params.put("payment",payment);}
        if(keyword!=null&&!keyword.isBlank()){where+=" and (lower(o.orderNumber) like :keyword escape '!' or lower(concat(concat(c.lastName,' '),c.firstName)) like :keyword escape '!')";params.put("keyword",AdminQuery.pattern(keyword));}
        if(from!=null){where+=" and o.createdAt>=:from";params.put("from",from.atStartOfDay());}
        if(to!=null){where+=" and o.createdAt<:to";params.put("to",to.plusDays(1).atStartOfDay());}
        if(refunds)where+=" and o.paymentStatus=com.petcarehub.enums.PaymentStatus.COMPLETED and (o.refundRequestedAt is not null or o.status=com.petcarehub.enums.OrderStatus.CANCELLED)";
        return query.page("Order o join o.customer c","o.id,o.orderNumber,concat(concat(c.lastName,' '),c.firstName),o.status,o.paymentStatus,o.totalAmount,o.createdAt,o.refundRequestedAt",
            "id,orderNumber,customer,status,paymentStatus,total,createdAt,refundRequestedAt",where,params,page,Map.of("id","o.id","createdAt","o.createdAt","total","o.totalAmount","status","o.status"));
    }
    public record Detail(OrderDTO order,java.math.BigDecimal total,java.math.BigDecimal taxAmount,LocalDateTime paidAt,LocalDateTime refundRequestedAt,LocalDateTime refundedAt,String refundReference) {}
    public Detail detail(Long id) { return detail(id,false); }
    public Detail detail(Long id,boolean print) {
        var order=find(id);var dto=service.getOrderById(id);
        audit.record(print?"ORDER_PRINT_READ":"ORDER_DETAIL_READ","ORDER",id,Map.of());
        return new Detail(dto,order.getTotalAmount(),order.getTaxAmount(),order.getPaidAt(),order.getRefundRequestedAt(),order.getRefundedAt(),order.getRefundReference());
    }
    private Order find(Long id){return orders.findById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn"));}
    private Order locked(Long id){return orders.findLockedById(id).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy đơn"));}
    private void reason(String reason){if(reason==null||reason.isBlank())throw new BadRequestException("Cần nhập lý do");}
    public OrderDTO transition(Long id,OperationRequests.Action request) {
        var order=locked(id);String before=order.getStatus().name();
        if("cancel".equals(request.action()))reason(request.reason());
        OrderDTO result=switch(request.action()){
            case "confirm"->service.confirmOrder(id);case "process"->service.processOrder(id);case "ship"->service.shipOrder(id);
            case "deliver"->service.deliverOrder(id);case "cancel"->service.cancelOrder(id);default->throw new BadRequestException("Thao tác không hợp lệ");};
        if(request.reason()!=null&&!request.reason().isBlank())note(order,request.reason());
        audit.record("ORDER_"+request.action().toUpperCase(Locale.ROOT),"ORDER",id,Map.of("statusBefore",before,"statusAfter",result.getStatus(),"reasonProvided",request.reason()!=null));
        return result;
    }
    public void requestRefund(Long id,OperationRequests.Note request) {
        var order=locked(id);
        if(order.getPaymentStatus()!=PaymentStatus.COMPLETED || !(order.getStatus()==OrderStatus.DELIVERED||order.getStatus()==OrderStatus.CANCELLED))throw new IllegalStateException("Chỉ lập yêu cầu hoàn cho đơn đã thu tiền và đã giao hoặc đã hủy");
        if(order.getRefundRequestedAt()==null){order.setRefundRequestedAt(LocalDateTime.now(clock));note(order,request.body());}
        audit.record("ORDER_REFUND_REQUESTED","ORDER",id,Map.of("reasonProvided",true));
    }
    public void refund(Long id,OperationRequests.Refund request) {
        var order=locked(id);String reference=request.reference().strip();
        boolean replay=order.getPaymentStatus()==PaymentStatus.REFUNDED;
        if(replay&&!reference.equals(order.getRefundReference()))throw new IllegalStateException("Đơn đã được hoàn với biên nhận khác");
        if(!replay){
            if(order.getPaymentStatus()!=PaymentStatus.COMPLETED || (order.getRefundRequestedAt()==null&&order.getStatus()!=OrderStatus.CANCELLED))throw new IllegalStateException("Đơn không ở hàng đợi hoàn tiền");
            order.setPaymentStatus(PaymentStatus.REFUNDED);order.setRefundReference(reference);order.setRefundedAt(LocalDateTime.now(clock));
            event(order,"CASH_REFUNDED",reference);note(order,request.reason());
        }
        audit.record("ORDER_REFUNDED","ORDER",id,Map.of("replay",replay,"paymentStatus","REFUNDED","reasonProvided",true));
    }
    private void event(Order order,String type,String reference){
        if(em.createQuery("select count(e) from OrderPaymentEvent e where e.orderId=:id and e.eventType=:type",Long.class).setParameter("id",order.getId()).setParameter("type",type).getSingleResult()==0)
            em.persist(OrderPaymentEvent.builder().orderId(order.getId()).eventType(type).amount(order.getTotalAmount()).reference(reference).actorId(ownership.userId()).occurredAt(LocalDateTime.now(clock)).build());
    }
    private void note(Order order,String text){em.persist(OrderNote.builder().orderId(order.getId()).actorId(ownership.userId()).body(text.strip()).createdAt(LocalDateTime.now(clock)).build());}
    public void addNote(Long id,OperationRequests.Note request){note(locked(id),request.body());audit.record("ORDER_NOTE_ADDED","ORDER",id,Map.of());}
    public Page<Map<String,Object>> notes(Long id,Pageable page){find(id);audit.record("ORDER_NOTES_READ","ORDER",id,Map.of());return query.page("OrderNote o","o.id,o.actorId,o.body,o.createdAt","id,actorId,body,createdAt","o.orderId=:id",Map.of("id",id),page,Map.of("id","o.id"));}
    public Page<Map<String,Object>> payments(Long id,Pageable page){find(id);audit.record("ORDER_PAYMENTS_READ","ORDER",id,Map.of());return query.page("OrderPaymentEvent o","o.id,o.eventType,o.amount,o.reference,o.occurredAt,o.actorId","id,type,amount,reference,occurredAt,actorId","o.orderId=:id",Map.of("id",id),page,Map.of("id","o.id"));}
}
