package com.petcarehub.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
@Component @RequiredArgsConstructor
public class NotificationJobs {
    private final NotificationDispatch dispatch;
    private final CustomerReminders reminders;
    private final JobMonitor monitor;
    @Value("${app.notifications.enabled:true}") private boolean enabled;
    @Value("${app.reminders.enabled:true}") private boolean remindersEnabled;
    @Scheduled(fixedDelayString="${app.notifications.interval-ms:5000}",initialDelayString="${app.notifications.initial-delay-ms:30000}")
    public void dispatch(){
        if(!enabled)return;
        Long run=monitor.start("NOTIFICATION_DISPATCH"),batch=null;
        int count=0;
        boolean failed=false;
        try{
            batch=dispatch.next();
            if(batch!=null)count=dispatch.process(batch);
        }
        catch(RuntimeException e){
            failed=true;
            if(batch!=null)dispatch.failed(batch);
        }
        finally{
            monitor.finish(run,count,failed);
        }
    }
    @Scheduled(fixedDelayString="${app.reminders.interval-ms:3600000}",initialDelayString="${app.reminders.initial-delay-ms:60000}")
    public void reminders(){
        if(!remindersEnabled)return;
        Long run=monitor.start("CUSTOMER_REMINDERS");
        int count=0;
        boolean failed=false;
        try{
            count=reminders.run();
        }
        catch(RuntimeException e){
            failed=true;
        }
        finally{
            monitor.finish(run,count,failed);
        }
    }
}
