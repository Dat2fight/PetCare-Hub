package com.petcarehub.service;
import com.petcarehub.entity.DataExportJob;
import com.petcarehub.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor
public class DataExportWorker {
    private final EntityManager em;
    private final Clock clock;
    private final ObjectMapper json;
    @Transactional public void expire(){
        var now=LocalDateTime.now(clock);
        em.createQuery("update DataExportJob j set j.status='EXPIRED',j.payload=null where j.expiresAt<=:now and j.status in ('QUEUED','READY')").setParameter("now",now).executeUpdate();
    }
    @Transactional(readOnly=true) public Long next(){
        var ids=em.createQuery("select j.id from DataExportJob j where j.status='QUEUED' order by j.id",Long.class).setMaxResults(1).getResultList();
        return ids.isEmpty()?null:ids.get(0);
    }
    @Transactional public int process(){
        expire();
        Long id=next();
        return id==null?0:process(id);
    }
    @Transactional public int process(Long id){
        var j=em.find(DataExportJob.class,id,LockModeType.PESSIMISTIC_WRITE);
        em.refresh(j);
        if(!j.getStatus().equals("QUEUED"))return 0;
        if(!LocalDateTime.now(clock).isBefore(j.getExpiresAt())){
            j.setStatus("EXPIRED");
            j.setPayload(null);
            return 0;
        }
        try{
            var in=new CatalogInput(json.readTree(j.getCriteria()));
            String keyword=in.text("keyword",100,false),hql;
            String[] headers;
            if(j.getKind().equals("revenue")){
                hql="select o.id,o.orderNumber,o.paidAt,o.totalAmount-coalesce(o.returnedAmount,0) from Order o where"+OrderRepository.REVENUE_FILTER+" order by o.id";
                headers=new String[]{
                    "ID","Mã đơn","Ngày thu","Doanh thu sau hoàn sản phẩm"
                }
                ;
            }
            else if(j.getKind().equals("inventory")){
                hql="select p.id,p.name,p.sku,i.quantity from Inventory i join i.product p where 1=1";
                headers=new String[]{
                    "ID","Sản phẩm","SKU","Tồn kho"
                }
                ;
            }
            else{
                hql="select p.id,p.firstName,p.lastName,p.enabled,p.createdAt from User p where p.deletedAt is null and exists(select r.id from p.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER)";
                headers=new String[]{
                    "ID","Tên","Họ","Hoạt động","Ngày tạo"
                }
                ;
            }
            if(!j.getKind().equals("revenue"))hql+=(keyword==null?"":" and lower("+(j.getKind().equals("inventory")?"p.name":"concat(p.firstName,' ',p.lastName)")+") like :word escape '!'")+" order by p.id";
            var q=em.createQuery(hql,Object[].class);
            if(j.getKind().equals("revenue")){
                var p=new ReportPeriod(in.date("from",true),in.date("to",true));
                q.setParameter("from",p.start());
                q.setParameter("to",p.end());
            }
            else if(keyword!=null)q.setParameter("word",AdminQuery.pattern(keyword));
            var rows=q.setMaxResults(20001).getResultList();
            if(rows.size()>20000){
                j.setStatus("FAILED");
                j.setErrorCode("EXPORT_LIMIT_EXCEEDED");
                return 0;
            }
            StringBuilder csv=new StringBuilder("\uFEFF");
            line(csv,headers);
            for(Object[] row:rows)line(csv,row);
            if(csv.length()>10000000){
                j.setStatus("FAILED");
                j.setErrorCode("EXPORT_LIMIT_EXCEEDED");
                return 0;
            }
            j.setPayload(csv.toString());
            j.setRowCount(rows.size());
            j.setStatus("READY");
            return rows.size();
        }
        catch(java.io.IOException|com.petcarehub.exception.BadRequestException e){
            j.setStatus("FAILED");
            j.setErrorCode("EXPORT_INPUT_INVALID");
            return 0;
        }
    }
    @Transactional public void failed(Long id){
        var j=em.find(DataExportJob.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(j!=null&&j.getStatus().equals("QUEUED")){
            j.setStatus("FAILED");
            j.setPayload(null);
            j.setErrorCode("EXPORT_FAILED");
        }
    }
    private void line(StringBuilder out,Object[] values){
        for(int i=0;i<values.length;i++){
            if(i>0)out.append(',');
            out.append(AdminCsvExport.cell(values[i]));
        }
        out.append("\r\n");
    }
}
