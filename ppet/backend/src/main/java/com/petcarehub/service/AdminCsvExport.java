package com.petcarehub.service;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @PreAuthorize("@perm.has('REPORT_VIEW')")
public class AdminCsvExport {
    private final EntityManager em;
    private final AdminInsightsService insights;
    private final AdminAuditService audit;
    private final Clock clock;
    public record Export(String filename,StreamingResponseBody body){
    }
    @Transactional public Export export(String kind,LocalDate from,LocalDate to,String keyword){
        var rows=new ArrayList<List<?>>();
        Map<String,Object> details=new HashMap<>();
        details.put("exportKind",kind);
        if(kind.equals("revenue")){
            var period=new ReportPeriod(from,to);
            rows.add(List.of("Ngày","Doanh thu đã thanh toán","Số đơn"));
            for(var r:insights.revenue("day",period))rows.add(List.of(r.get("date"),r.get("revenue"),r.get("orders")));
            details.put("from",from.toString());
            details.put("to",to.toString());
        }
        else if(Set.of("inventory","customers").contains(kind)){
            String hql=kind.equals("inventory")?"select p.id,p.name,p.sku,i.quantity,coalesce(i.reorderPoint,10) from Inventory i join i.product p where 1=1":"select p.id,p.firstName,p.lastName,p.enabled,p.createdAt from User p where exists(select r.id from p.roles r where r.name=com.petcarehub.enums.RoleType.CUSTOMER) and p.deletedAt is null";
            var q=em.createQuery(hql+(keyword==null||keyword.isBlank()?"":" and lower("+(kind.equals("inventory")?"p.name":"concat(p.firstName,' ',p.lastName)")+") like :keyword escape '!'")+" order by p.id",Object[].class);
            if(keyword!=null&&!keyword.isBlank())q.setParameter("keyword",AdminQuery.pattern(keyword));
            var values=q.setMaxResults(5001).getResultList();
            if(values.size()>5000)throw CatalogInput.bad("Xuất tối đa 5000 dòng; hãy thu hẹp bộ lọc");
            rows.add(kind.equals("inventory")?List.of("ID","Sản phẩm","SKU","Tồn kho","Ngưỡng thấp"):List.of("ID","Tên","Họ","Hoạt động","Ngày tạo"));
            for(var r:values)rows.add(Arrays.asList(r));
            details.put("keywordProvided",keyword!=null&&!keyword.isBlank());
        }
        else throw CatalogInput.bad("Loại xuất không hợp lệ");
        details.put("count",rows.size()-1);
        audit.record("CSV_EXPORT_PREPARED","REPORT",null,details);
        return new Export("petcare-"+kind+"-"+LocalDate.now(clock)+".csv",output->{
            var writer=new OutputStreamWriter(output,StandardCharsets.UTF_8);writer.write('\uFEFF');for(var row:rows){
                for(int i=0;i<row.size();i++){
                    if(i>0)writer.write(',');writer.write(cell(row.get(i)));
                }
                writer.write("\r\n");
            }
            writer.flush();
        }
        );
    }
    public static String cell(Object value){
        String s=value==null?"":value.toString();
        String trimmed=s.stripLeading();
        if(!s.isEmpty()&&(s.charAt(0)=='\t'||s.charAt(0)=='\r'||!trimmed.isEmpty()&&"=+-@".indexOf(trimmed.charAt(0))>=0))s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
}
