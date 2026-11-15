package com.petcarehub;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.service.*;
import com.petcarehub.security.UserDetailsImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class PhaseFiveConcurrencyTest {
    @Autowired EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired ReturnsService returns;
    @Autowired DataExportsService exports;
    @Autowired ObjectMapper json;
    @Test void competingStaffCannotRestockRefundOrConsumeTwice() throws Exception {
        var tx=new TransactionTemplate(manager);
        Map<String,Long> ids=new HashMap<>();
        User actor=tx.execute(status->{
            var roles=em.createQuery("from Role where name=:name",Role.class).setParameter("name",RoleType.ADMIN).getResultList();
            Role role;if(roles.isEmpty()){
                role=new Role();role.setName(RoleType.ADMIN);em.persist(role);
            }
            else role=roles.get(0);
            String tag=UUID.randomUUID().toString();var u=User.builder().username(tag).email(tag+"@example.test").firstName("Concurrent").lastName("Test").passwordHash("unused").roles(new HashSet<>(Set.of(role))).build();em.persist(u);ids.put("user",u.getId());
            var c=ProductCategory.builder().name(tag).build();em.persist(c);ids.put("category",c.getId());
            var p=Product.builder().name(tag).sku(tag).category(c).price(BigDecimal.TEN).build();em.persist(p);ids.put("product",p.getId());
            var i=Inventory.builder().product(p).quantity(0).build();em.persist(i);ids.put("inventory",i.getId());
            var o=Order.builder().customer(u).orderNumber(tag).subtotal(BigDecimal.TEN).totalAmount(BigDecimal.TEN).status(OrderStatus.DELIVERED).paymentStatus(PaymentStatus.COMPLETED).build();
            o.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(p).quantity(1).unitPrice(BigDecimal.TEN).subtotal(BigDecimal.TEN).build());em.persist(o);em.flush();ids.put("order",o.getId());
            var r=new ReturnRequest();r.setOrderId(o.getId());r.setItemId(o.getItems().get(0).getId());r.setCustomerId(u.getId());r.setQuantity(1);r.setStatus("APPROVED");r.setReason("Concurrent return");r.setRequestKey(tag);r.setFingerprint("a".repeat(64));r.setMaximumRefund(BigDecimal.TEN);r.setCreatedAt(LocalDateTime.now());em.persist(r);ids.put("return",r.getId());
            var e=new DataExportJob();e.setActorId(u.getId());e.setRequestKey(tag);e.setFingerprint("b".repeat(64));e.setKind("customers");e.setCriteria("{}");e.setStatus("READY");e.setPayload("\uFEFFID\r\n1\r\n");e.setRowCount(1);e.setCreatedAt(LocalDateTime.now());e.setExpiresAt(LocalDateTime.now().plusDays(1));em.persist(e);ids.put("export",e.getId());return u;
        }
        );
        try {
            var receive=json.valueToTree(Map.of("action","receive","reason","Actual receipt"));
            assertEquals(2,race(actor,()->{
                returns.transition(ids.get("return"),receive);return 1;
            }
            ));
            tx.executeWithoutResult(s->assertEquals(1,em.find(Inventory.class,ids.get("inventory")).getQuantity()));
            var refund=json.valueToTree(Map.of("action","refund","reason","Cash paid","amount",10,"reference","RACE-REFUND"));
            assertEquals(2,race(actor,()->{
                returns.transition(ids.get("return"),refund);return 1;
            }
            ));
            tx.executeWithoutResult(s->{
                assertEquals(0,em.find(Order.class,ids.get("order")).getReturnedAmount().compareTo(BigDecimal.TEN));assertEquals(1L,em.createQuery("select count(e) from OrderPaymentEvent e where e.orderId=:id",Long.class).setParameter("id",ids.get("order")).getSingleResult());
            }
            );
            assertEquals(1,race(actor,()->{
                try{
                    exports.download(ids.get("export"));return 1;
                }
                catch(IllegalStateException consumed){
                    return 0;
                }
            }
            ));
        }
        finally {
            tx.executeWithoutResult(s->{
                em.createQuery("delete from AdminAuditLog where actorUserId=:id").setParameter("id",ids.get("user")).executeUpdate();
                em.createQuery("delete from Notification where user.id=:id").setParameter("id",ids.get("user")).executeUpdate();
                em.createQuery("delete from DataExportJob where id=:id").setParameter("id",ids.get("export")).executeUpdate();
                em.createQuery("delete from ReturnRequest where id=:id").setParameter("id",ids.get("return")).executeUpdate();
                em.createQuery("delete from OrderPaymentEvent where orderId=:id").setParameter("id",ids.get("order")).executeUpdate();
                em.createQuery("delete from InventoryTransaction where inventory.id=:id").setParameter("id",ids.get("inventory")).executeUpdate();
                em.remove(em.find(Order.class,ids.get("order")));em.flush();em.remove(em.find(Inventory.class,ids.get("inventory")));em.flush();em.remove(em.find(Product.class,ids.get("product")));em.flush();em.remove(em.find(ProductCategory.class,ids.get("category")));em.remove(em.find(User.class,ids.get("user")));
            }
            );
        }
    }
    private int race(User actor,Callable<Integer> work)throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        var gate=new CountDownLatch(1);
        Callable<Integer> task=()->{
            gate.await();
            var principal=UserDetailsImpl.build(actor);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
            try{
                return work.call();
            }
            finally{
                SecurityContextHolder.clearContext();
            }
        }
        ;
        try{
            var one=pool.submit(task);
            var two=pool.submit(task);
            gate.countDown();
            return one.get(20,TimeUnit.SECONDS)+two.get(20,TimeUnit.SECONDS);
        }
        finally{
            pool.shutdownNow();
        }
    }
}
