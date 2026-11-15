package com.petcarehub.service;
import java.util.*;
import java.util.regex.*;
import com.petcarehub.entity.NotificationTemplate;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
public class NotificationText {
    private final EntityManager em;
    public static final Set<String> RESERVED=Set.of("GENERAL","APPOINTMENT_REMINDER","VACCINATION_REMINDER");
    private static final Pattern VARIABLE=Pattern.compile("\\{([^{}]+)}");
    public static void validate(String text,String purpose){
        if(text==null||text.isBlank()||text.indexOf('<')>=0||text.indexOf('>')>=0)throw CatalogInput.bad("Nội dung phải là văn bản thuần, không có thẻ HTML");
        var allowed=switch(purpose){
            case "GENERAL"->Set.of("firstName");
            case "APPOINTMENT"->Set.of("firstName","petName","appointmentTime");
            case "VACCINATION"->Set.of("firstName","petName","dueDate");
            default->throw CatalogInput.bad("Loại mẫu không hợp lệ");
        }
        ;
        var m=VARIABLE.matcher(text);
        while(m.find())if(!allowed.contains(m.group(1)))throw CatalogInput.bad("Biến mẫu không được hỗ trợ: "+m.group(1));
        String remainder=m.replaceAll("");
        if(remainder.contains("{")||remainder.contains("}"))throw CatalogInput.bad("Cú pháp biến mẫu không hợp lệ");
    }
    public static String render(String text,Map<String,String> values,int limit){
        var m=VARIABLE.matcher(text);
        StringBuffer out=new StringBuffer();
        while(m.find()){
            String value=values.get(m.group(1));
            if(value==null)throw CatalogInput.bad("Thiếu dữ liệu cho biến mẫu");
            value=value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
            m.appendReplacement(out,Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        if(out.length()>limit)throw CatalogInput.bad("Nội dung sau thay biến vượt giới hạn");
        return out.toString();
    }
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional public void initialize(){
        for(var row:List.of(new String[]{
            "GENERAL","GENERAL","Thông báo từ PetCare Hub","Xin chào {firstName}, cảm ơn bạn đã đồng hành cùng PetCare Hub."
        }
        ,new String[]{
            "APPOINTMENT_REMINDER","APPOINTMENT","Nhắc lịch hẹn","{petName} có lịch hẹn lúc {appointmentTime}. Vui lòng đến đúng giờ."
        }
        ,new String[]{
            "VACCINATION_REMINDER","VACCINATION","Nhắc lịch tiêm","{petName} đến lịch tiêm tiếp theo ngày {dueDate}."
        }
        )){
            if(em.createQuery("select count(t) from NotificationTemplate t where t.code=:code",Long.class).setParameter("code",row[0]).getSingleResult()==0){
                var t=new NotificationTemplate();
                t.setCode(row[0]);
                t.setPurpose(row[1]);
                t.setTitle(row[2]);
                t.setBody(row[3]);
                em.persist(t);
            }
        }
    }
}
