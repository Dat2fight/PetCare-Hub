package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;
import java.util.*;
@Service @RequiredArgsConstructor
public class SettingsService {
    private final EntityManager em;
    private final CatalogMutex mutex;
    private final AdminAuditService audit;
    private final com.petcarehub.config.BookingProperties booking;
    private final com.petcarehub.config.OrderExpiryConfig.Settings expiry;
    @Value("${app.checkout.shipping-fee:30000}") private BigDecimal shipping;
    @Value("${app.checkout.free-shipping-from:500000}") private BigDecimal freeFrom;
    @Value("${app.returns.window-days:7}") private int returnDays;
    private volatile Map<String,BigDecimal> cached;
    private volatile long cacheUntil;
    private long generation;
    public static final Set<String> KEYS=Set.of("shippingFee","freeShippingFrom","holdHours","cancellationHours","returnDays");
    @Transactional(readOnly=true)
    public Map<String,BigDecimal> current() {
        // Publish cache fills only after commit; local writes invalidate after commit.
        if(cached!=null && TransactionSynchronizationManager.isCurrentTransactionReadOnly() && System.nanoTime()<cacheUntil) return cached;
        long observed;
        synchronized(this){
            observed=generation;
        }
        Map<String,BigDecimal> values=new HashMap<>(Map.of("shippingFee",shipping,"freeShippingFrom",freeFrom,
        "holdHours",BigDecimal.valueOf(expiry.holdHours()),"cancellationHours",BigDecimal.valueOf(booking.getCancellationNoticeHours()),"returnDays",BigDecimal.valueOf(returnDays)));
        for(var row:em.createQuery("from SystemSetting",SystemSetting.class).setMaxResults(KEYS.size()).getResultList())
        if(KEYS.contains(row.getSettingKey()))values.put(row.getSettingKey(),row.getSettingValue());
        var result=Map.copyOf(values);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){
                synchronized(SettingsService.this){
                    if(observed==generation){
                        cached=result;cacheUntil=System.nanoTime()+5_000_000_000L;
                    }
                }
            }
        }
        );
        return result;
    }
    @Transactional(readOnly=true) public int integer(String key){
        return current().get(key).intValueExact();
    }
    // Loyalty thresholds retain the existing canonical row, including edits from phase 3.
    @Transactional(readOnly=true) public Map<String,Integer> thresholds(){
        var r=em.find(LoyaltyRules.class,1);
        if(r==null)r=new LoyaltyRules();
        return Map.of("BRONZE",0,"SILVER",r.getSilver(),"GOLD",r.getGold(),"PLATINUM",r.getPlatinum());
    }
    @Transactional @PreAuthorize("@perm.has('SYSTEM_MANAGE')")
    public void update(JsonNode body){
        mutex.write();
        var in=new CatalogInput(body);
        in.keys(String.join(",",KEYS));
        if(body.size()==0)throw CatalogInput.bad("Cần ít nhất một cấu hình");
        Map<String,String> changes=new TreeMap<>();
        for(String key:KEYS)if(body.has(key)){
            BigDecimal value=in.money(key,false);
            if(Set.of("holdHours","cancellationHours","returnDays").contains(key)){
                int max=key.equals("holdHours")?168:key.equals("returnDays")?90:168;
                if(value.stripTrailingZeros().scale()>0||value.compareTo(BigDecimal.valueOf(max))>0||!key.equals("cancellationHours")&&value.signum()==0)throw CatalogInput.bad(key+": ngoài giới hạn");
            }
            else if(value.compareTo(BigDecimal.valueOf(1000000000))>0)throw CatalogInput.bad("Giới hạn tiền là 1 tỷ");
            var row=em.find(SystemSetting.class,key);
            BigDecimal before=row==null?fallback(key):row.getSettingValue();
            if(row==null){
                row=new SystemSetting();
                row.setSettingKey(key);
                row.setSettingValue(value);
                em.persist(row);
            }
            else row.setSettingValue(value);
            // All changed numeric keys are collected in one safe audit, below.
            changes.put(key,before.toPlainString()+" -> "+value.toPlainString());
        }
        audit.record("SYSTEM_SETTINGS_UPDATED","SYSTEM_SETTINGS",null,Map.of("changes",changes.toString()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){
                synchronized(SettingsService.this){
                    generation++;cached=null;cacheUntil=0;
                }
            }
        }
        );
    }
    private BigDecimal fallback(String key){
        return switch(key){
            case "shippingFee"->shipping;
            case "freeShippingFrom"->freeFrom;
            case "holdHours"->BigDecimal.valueOf(expiry.holdHours());
            case "cancellationHours"->BigDecimal.valueOf(booking.getCancellationNoticeHours());
            default->BigDecimal.valueOf(returnDays);
        }
        ;
    }
}
