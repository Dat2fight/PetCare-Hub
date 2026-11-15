package com.petcarehub.service;
import com.petcarehub.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.*;
import java.math.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) @PreAuthorize("@perm.has('REPORT_VIEW')")
public class AdminInsightsService {
    private final EntityManager em;
    private final OrderRepository orders;
    private final Clock clock;
    public static final String REVENUE_DEFINITION="Chỉ đơn thanh toán COMPLETED, loại đơn hủy/hoàn; tiền mặt COD phải DELIVERED. Ghi nhận theo paidAt, gồm tổng tiền thực thu của cả sản phẩm và thú cưng.";
    public ReportPeriod period(String name,LocalDate from,LocalDate to){
        return ReportPeriod.of(name,from,to,clock);
    }
    private long count(String from,String where,Map<String,Object> params){
        var q=em.createQuery("select count(o) from "+from+" o where "+where,Long.class);
        params.forEach(q::setParameter);
        return q.getSingleResult();
    }
    private Map<String,Object> bounds(ReportPeriod p){
        return Map.of("from",p.start(),"to",p.end());
    }
    private long created(String entity,String extra,ReportPeriod p){
        return count(entity,"o.createdAt>=:from and o.createdAt<:to"+extra,bounds(p));
    }
    public record Metric(BigDecimal current,BigDecimal previous,BigDecimal change,BigDecimal percent){
    }
    private Metric metric(Number current,Number previous){
        var c=new BigDecimal(current.toString());
        var p=new BigDecimal(previous.toString());
        return new Metric(c,p,c.subtract(p),p.signum()==0?(c.signum()==0?BigDecimal.ZERO:null):c.subtract(p).multiply(BigDecimal.valueOf(100)).divide(p,2,RoundingMode.HALF_UP));
    }
    public Map<String,Object> summary(String name,LocalDate from,LocalDate to){
        var p=period(name,from,to);
        var previous=p.previous();
        String customer=" and exists(select r.id from o.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER)";
        String pending=" and o.status in (com.petcarehub.enums.OrderStatus.PENDING,com.petcarehub.enums.OrderStatus.CONFIRMED,com.petcarehub.enums.OrderStatus.PROCESSING)";
        var today=LocalDate.now(clock);
        return Map.of("from",p.from(),"to",p.to(),"previousFrom",previous.from(),"previousTo",previous.to(),"zone",clock.getZone().getId(),"definition",REVENUE_DEFINITION,"metrics",Map.of(
        "revenue",metric(orders.summarizeRevenue(p.start(),p.end()).getRevenue(),orders.summarizeRevenue(previous.start(),previous.end()).getRevenue()),
        "orders",metric(created("Order","",p),created("Order","",previous)),
        "pending",metric(created("Order",pending,p),created("Order",pending,previous)),
        "customers",metric(created("User",customer,p),created("User",customer,previous)),
        "appointmentsToday",metric(count("Appointment","o.appointmentDate=:date",Map.of("date",today)),count("Appointment","o.appointmentDate=:date",Map.of("date",today.minusDays(1))))));
    }
    public List<Map<String,Object>> revenue(String granularity,ReportPeriod p){
        if(!Set.of("day","month").contains(granularity))throw CatalogInput.bad("Chọn day hoặc month");
        String grouping="year(o.paidAt),month(o.paidAt)"+(granularity.equals("day")?",day(o.paidAt)":"");
        var q=em.createQuery("select "+grouping+",coalesce(sum(o.totalAmount),0),count(o) from Order o where"+OrderRepository.REVENUE_FILTER+" group by "+grouping,Object[].class).setParameter("from",p.start()).setParameter("to",p.end());
        Map<LocalDate,Object[]> byDate=new HashMap<>();
        boolean day=granularity.equals("day");
        for(var r:q.getResultList())byDate.put(LocalDate.of(((Number)r[0]).intValue(),((Number)r[1]).intValue(),day?((Number)r[2]).intValue():1),r);
        var result=new ArrayList<Map<String,Object>>();
        for(LocalDate date=day?p.from():p.from().withDayOfMonth(1);!date.isAfter(p.to());date=day?date.plusDays(1):date.plusMonths(1)){
            var r=byDate.get(date);
            int at=day?3:2;
            result.add(Map.of("date",date,"revenue",r==null?BigDecimal.ZERO:r[at],"orders",r==null?0:r[at+1]));
        }
        return result;
    }
    public Map<String,Object> rankings(ReportPeriod p){
        var products=em.createQuery("select p.id,p.name,sum(i.quantity),sum(i.subtotal) from OrderItem i join i.order o join i.product p where"+OrderRepository.REVENUE_FILTER+" group by p.id,p.name order by sum(i.quantity) desc,p.id",Object[].class).setParameter("from",p.start()).setParameter("to",p.end()).setMaxResults(10).getResultList().stream().map(r->Map.of("id",r[0],"name",r[1],"quantity",r[2],"gross",r[3])).toList();
        var services=em.createQuery("select s.id,s.name,count(a) from Appointment a join a.service s where a.appointmentDate>=:from and a.appointmentDate<=:to and a.status<>com.petcarehub.enums.AppointmentStatus.CANCELLED group by s.id,s.name order by count(a) desc,s.id",Object[].class).setParameter("from",p.from()).setParameter("to",p.to()).setMaxResults(10).getResultList().stream().map(r->Map.of("id",r[0],"name",r[1],"count",r[2])).toList();
        long total=created("Order","",p),cancelled=created("Order"," and o.status=com.petcarehub.enums.OrderStatus.CANCELLED",p),expired=created("Order"," and o.expiredAt is not null",p);
        return Map.of("products",products,"services",services,"totalOrders",total,"cancelled",cancelled,"expired",expired,"cancellationPercent",total==0?0:100.0*cancelled/total,"expiryPercent",total==0?0:100.0*expired/total);
    }
    public List<Map<String,Object>> alerts(){
        var now=LocalDateTime.now(clock);
        return List.of(
        alert("lowStock","Sản phẩm tồn thấp",count("Inventory","o.product.active=true and o.quantity<=coalesce(o.reorderPoint,10)",Map.of()),"/admin/inventory?low=true"),
        alert("refunds","Chờ hoàn tiền",count("Order",AdminOrderOperations.REFUND_QUEUE,Map.of()),"/admin/orders?refunds=true"),
        alert("reviews","Đánh giá chờ duyệt",count("Review","o.moderationState='PENDING'",Map.of()),"/admin/reviews"),
        alert("unassigned","Lịch chưa phân công",count("Appointment","o.staff is null and o.appointmentDate>=:date and o.status in (com.petcarehub.enums.AppointmentStatus.SCHEDULED,com.petcarehub.enums.AppointmentStatus.CONFIRMED)",Map.of("date",now.toLocalDate())),"/admin/appointments?unassigned=true&actionable=true"),
        alert("expiring","Đơn giữ chỗ sắp hết hạn (2 giờ)",count("Order","o.status=com.petcarehub.enums.OrderStatus.PENDING and o.paymentStatus=com.petcarehub.enums.PaymentStatus.PENDING and o.expiresAt>=:now and o.expiresAt<=:soon",Map.of("now",now,"soon",now.plusHours(2))),"/admin/orders?expiring=true"));
    }
    private Map<String,Object> alert(String key,String label,long count,String href){
        return Map.of("key",key,"label",label,"count",count,"href",href);
    }
}
