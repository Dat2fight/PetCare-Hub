package com.petcarehub.service;
import com.petcarehub.dto.admin.OperationRequests;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.enums.TransactionType;
import com.petcarehub.exception.*;
import com.petcarehub.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('INVENTORY_MANAGE')")
public class AdminInventoryOperations {
    private final InventoryService service;private final InventoryRepository inventory;private final AdminQuery query;private final AdminAuditService audit;
    @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(String keyword,boolean low,Pageable page){
        String where="o.product is not null";Map<String,Object> params=new HashMap<>();
        if(keyword!=null&&!keyword.isBlank()){where+=" and (lower(p.name) like :keyword escape '!' or lower(p.sku) like :keyword escape '!')";params.put("keyword",AdminQuery.pattern(keyword));}
        if(low)where+=" and o.quantity<=coalesce(o.reorderPoint,10)";
        return query.page("Inventory o join o.product p","o.id,p.id,p.name,p.sku,o.quantity,o.reorderPoint,o.updatedAt","id,productId,name,sku,quantity,reorderPoint,updatedAt",where,params,page,Map.of("id","o.id","quantity","o.quantity","name","p.name"));
    }
    public void move(OperationRequests.Stock request){
        var item=inventory.findLockedByProductId(request.productId()).orElseThrow(()->new ResourceNotFoundException("Không tìm thấy tồn kho"));
        if(!"ADJUSTMENT".equals(request.type())&&request.quantity()<=0)throw new BadRequestException("Nhập/xuất phải lớn hơn 0");
        if("STOCK_IN".equals(request.type())){var r=new StockUpdateRequest();r.setProductId(request.productId());r.setQuantity(request.quantity());r.setReason(request.reason());service.addStock(r);}
        else if("STOCK_OUT".equals(request.type()))service.reduceStockWithReason(request.productId(),request.quantity(),request.reason());
        else service.adjustStock(request.productId(),request.quantity(),request.reason());
        audit.record("INVENTORY_"+request.type(),"INVENTORY",item.getId(),Map.of("quantity",request.quantity(),"reasonProvided",true));
    }
    @Transactional(readOnly=true)
    public Page<Map<String,Object>> history(Long productId,TransactionType type,Long actor,LocalDate from,LocalDate to,Pageable page){
        AdminQuery.dates(from,to);String where="1=1";Map<String,Object> params=new HashMap<>();
        if(productId!=null){where+=" and i.product.id=:productId";params.put("productId",productId);}
        if(type!=null){where+=" and o.transactionType=:type";params.put("type",type);}
        if(actor!=null){where+=" and o.performedBy.id=:actor";params.put("actor",actor);}
        if(from!=null){where+=" and o.createdAt>=:from";params.put("from",from.atStartOfDay());}
        if(to!=null){where+=" and o.createdAt<:to";params.put("to",to.plusDays(1).atStartOfDay());}
        return query.page("InventoryTransaction o join o.inventory i left join o.performedBy a","o.id,i.product.id,o.transactionType,o.quantity,o.notes,o.source,o.referenceType,o.referenceId,a.id,o.createdAt",
            "id,productId,type,quantity,reason,source,referenceType,referenceId,actorId,createdAt",where,params,page,Map.of("id","o.id","createdAt","o.createdAt"));
    }
}
