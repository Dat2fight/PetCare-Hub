package com.petcarehub.service;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.*;
import com.petcarehub.exception.BadRequestException;
import java.util.*;
@Component @RequiredArgsConstructor
public class AdminQuery {
    private final EntityManager em;
    public Page<Map<String,Object>> page(String from,String columns,String names,String where,Map<String,Object> parameters,Pageable pageable,Map<String,String> sorts) {
        // SQL Server rejects repeated ORDER BY columns, including the ID tie-breaker.
        var order = new LinkedHashMap<String, String>();
        for (var sort : pageable.getSort()) {
            String column = sorts.get(sort.getProperty());
            if (column == null) throw new BadRequestException("Cột sắp xếp không hợp lệ");
            order.putIfAbsent(column, sort.isAscending() ? " asc" : " desc");
        }
        order.putIfAbsent("o.id", " desc");
        String orderBy = order.entrySet().stream()
                .map(entry -> entry.getKey() + entry.getValue())
                .collect(java.util.stream.Collectors.joining(","));
        int size=Math.min(pageable.getPageSize(),100);
        if(pageable.getOffset()>Integer.MAX_VALUE) throw new BadRequestException("Trang vượt giới hạn");
        // All query fragments and sort mappings are server constants. Every client value is bound.
        var query=em.createQuery("select "+columns+" from "+from+" where "+where+" order by "+orderBy,Object[].class);
        var count=em.createQuery("select count(o) from "+from+" where "+where,Long.class);
        parameters.forEach((key,value)->{query.setParameter(key,value);count.setParameter(key,value);});
        String[] fields=names.split(",");
        var rows=query.setFirstResult(pageable.getPageNumber()*size).setMaxResults(size).getResultList().stream().map(values->{Map<String,Object> row=new LinkedHashMap<>();for(int i=0;i<fields.length;i++)row.put(fields[i],values[i]);return row;}).toList();
        return new PageImpl<>(rows,PageRequest.of(pageable.getPageNumber(),size,pageable.getSort()),count.getSingleResult());
    }
    public static String pattern(String input) { if(input.length()>100) throw new BadRequestException("Từ khóa tối đa 100 ký tự"); return "%"+input.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_").replace("[","![")+"%"; }
    public static void dates(java.time.LocalDate from,java.time.LocalDate to) { if(from!=null&&to!=null&&from.isAfter(to)||to!=null&&to.getYear()>9998)throw new BadRequestException("Khoảng ngày không hợp lệ"); }
}
