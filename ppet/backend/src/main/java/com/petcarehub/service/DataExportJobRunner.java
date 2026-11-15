package com.petcarehub.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
@Component @RequiredArgsConstructor
public class DataExportJobRunner {
    private final DataExportWorker worker;
    private final JobMonitor monitor;
    @Value("${app.exports.enabled:true}") private boolean enabled;
    @Scheduled(fixedDelayString="${app.exports.interval-ms:5000}",initialDelayString="${app.exports.initial-delay-ms:30000}")
    public void run(){
        if(!enabled)return;
        Long run=monitor.start("DATA_EXPORT"),id=null;
        int count=0;
        boolean failed=false;
        try{
            worker.expire();
            id=worker.next();
            if(id!=null)count=worker.process(id);
        }
        catch(RuntimeException e){
            failed=true;
            if(id!=null)worker.failed(id);
        }
        finally{
            monitor.finish(run,count,failed);
        }
    }
}
