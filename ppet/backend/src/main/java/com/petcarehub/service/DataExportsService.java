package com.petcarehub.service;
import com.petcarehub.entity.DataExportJob;
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
import java.util.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('SYSTEM_MANAGE')")
public class DataExportsService {
    private final EntityManager em;
    private final CatalogMutex mutex;
    private final Ownership ownership;
    private final Clock clock;
    private final AdminAuditService audit;
    private final AdminQuery query;
    public Object create(JsonNode body){
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("kind,from,to,keyword,requestKey");
        String kind=in.text("kind",20,true);
        if(!Set.of("revenue","inventory","customers").contains(kind))throw CatalogInput.bad("Loại xuất không hỗ trợ");
        if(kind.equals("revenue"))new ReportPeriod(in.date("from",true),in.date("to",true));
        in.text("keyword",100,false);
        String key=in.text("requestKey",36,true);
        try{
            UUID.fromString(key);
        }
        catch(Exception e){
            throw CatalogInput.bad("requestKey phải là UUID");
        }
        long actor=ownership.userId();
        String fingerprint=CatalogInput.fingerprint(body.toString());
        var rows=em.createQuery("from DataExportJob j where j.actorId=:actor and j.requestKey=:key",DataExportJob.class).setParameter("actor",actor).setParameter("key",key).getResultList();
        if(!rows.isEmpty()){
            var old=rows.get(0);
            if(!old.getFingerprint().equals(fingerprint))throw new IllegalStateException("Mã yêu cầu đã dùng");
            audit.record("DATA_EXPORT_REQUESTED","DATA_EXPORT",old.getId(),Map.of("replay",true));
            return Map.of("id",old.getId());
        }
        if(em.createQuery("select count(j) from DataExportJob j where j.actorId=:actor and (j.status='QUEUED' or j.createdAt>:since)",Long.class).setParameter("actor",actor).setParameter("since",LocalDateTime.now(clock).minusMinutes(1)).getSingleResult()>0)throw new IllegalStateException("Chờ đợt trước hoàn tất và tối thiểu một phút");
        var j=new DataExportJob();
        j.setActorId(actor);
        j.setRequestKey(key);
        j.setFingerprint(fingerprint);
        j.setKind(kind);
        j.setCriteria(body.toString());
        j.setCreatedAt(LocalDateTime.now(clock));
        j.setExpiresAt(j.getCreatedAt().plusHours(24));
        em.persist(j);
        audit.record("DATA_EXPORT_REQUESTED","DATA_EXPORT",j.getId(),Map.of("exportKind",kind));
        return Map.of("id",j.getId());
    }
    @Transactional(readOnly=true) public Object history(Pageable p){
        return query.page("DataExportJob o","o.id,o.kind,o.status,o.rowCount,o.createdAt,o.expiresAt,o.consumedAt,o.errorCode","id,kind,status,rowCount,createdAt,expiresAt,consumedAt,errorCode","o.actorId=:actor",Map.of("actor",ownership.userId()),p,Map.of("id","o.id"));
    }
    public AdminCsvExport.Export download(long id){
        var j=em.find(DataExportJob.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(j==null||j.getActorId()!=ownership.userId().longValue())throw new ResourceNotFoundException("Không tìm thấy bản xuất");
        if(!j.getStatus().equals("READY")||!LocalDateTime.now(clock).isBefore(j.getExpiresAt()))throw new IllegalStateException("File chưa sẵn sàng, đã dùng hoặc hết hạn");
        String text=j.getPayload();
        j.setPayload(null);
        j.setStatus("CONSUMED");
        j.setConsumedAt(LocalDateTime.now(clock));
        audit.record("DATA_EXPORT_DOWNLOADED","DATA_EXPORT",id,Map.of("exportKind",j.getKind(),"count",j.getRowCount()));
        return new AdminCsvExport.Export("petcare-"+j.getKind()+"-"+LocalDate.now(clock)+".csv",out->{
            var writer=new java.io.OutputStreamWriter(out,java.nio.charset.StandardCharsets.UTF_8);writer.write(text);writer.flush();
        }
        );
    }
}
