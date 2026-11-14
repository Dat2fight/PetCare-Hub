package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;
import java.math.*;
@Service @RequiredArgsConstructor @Transactional @PreAuthorize("@perm.has('CATALOG_MANAGE')")
public class AdminProductBatch  {
    private final EntityManager em;
    private final AdminCatalogService catalog;
    private final CatalogMutex mutex;
    private final AdminAuditService audit;
    private final CatalogImagePolicy images;
    private final ObjectMapper mapper;
    public Map<String,Object> bulk(JsonNode body,boolean apply) {
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("ids,action,value,active,previewToken");
        var ids=new TreeSet<Long>();
        JsonNode input=body.get("ids");
        if(input==null||!input.isArray()||input.isEmpty()||input.size()>100)throw CatalogInput.bad("Chọn từ 1 đến 100 sản phẩm");
        for(var n:input) {
            if(!n.isIntegralNumber()||!n.canConvertToLong()||n.longValue()<1||!ids.add(n.longValue()))throw CatalogInput.bad("ID không hợp lệ hoặc trùng");
        }
        String action=in.text("action",30,true);
        if(!Set.of("ACTIVE","PRICE_AMOUNT","PRICE_PERCENT").contains(action))throw CatalogInput.bad("Thao tác không hợp lệ");
        BigDecimal delta=BigDecimal.ZERO;
        if(!action.equals("ACTIVE")) {
            var v=body.get("value");
            if(v==null||!v.isNumber()||v.decimalValue().scale()>2)throw CatalogInput.bad("Giá trị điều chỉnh không hợp lệ");
            delta=v.decimalValue();
            if(action.equals("PRICE_PERCENT")&&delta.abs().compareTo(BigDecimal.valueOf(100))>0)throw CatalogInput.bad("Phần trăm từ -100 đến 100");
        }
        var rows=new ArrayList<Map<String,Object>>();
        var items=new ArrayList<Product>();
        for(Long id:ids) {
            var p=em.find(Product.class,id,LockModeType.PESSIMISTIC_WRITE);
            if(p==null)throw CatalogInput.bad("Không tìm thấy sản phẩm #"+id);
            var price=action.equals("ACTIVE")?p.getPrice():p.getPrice().add(action.equals("PRICE_AMOUNT")?delta:p.getPrice().multiply(delta).movePointLeft(2)).setScale(2,RoundingMode.HALF_UP);
            if(price.signum()<0||price.compareTo(new BigDecimal("999999999999.99"))>0)throw CatalogInput.bad("Giá sau điều chỉnh ngoài giới hạn");
            rows.add(Map.of("id",id,"name",p.getName(),"before",p.getPrice(),"after",price,"activeBefore",p.getActive(),"activeAfter",action.equals("ACTIVE")?in.bool("active",false):p.getActive()));
            items.add(p);
        }
        String token=CatalogInput.fingerprint(rows.toString());
        if(apply) {
            if(!token.equals(in.text("previewToken",64,true)))throw new IllegalStateException("Giá/dữ liệu đã đổi, xem trước lại");
            for(int i=0;i<items.size();i++) {
                items.get(i).setPrice((BigDecimal)rows.get(i).get("after"));
                items.get(i).setActive((Boolean)rows.get(i).get("activeAfter"));
            }
        }
        audit.record(apply?"PRODUCT_BULK_APPLIED":"PRODUCT_BULK_PREVIEW","PRODUCT",null,Map.of("count",ids.size(),"ids",ids.toString()));
        return Map.of("rows",rows,"previewToken",token);
    }
    public Map<String,Object> csv(JsonNode body,boolean apply) {
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("csv,previewToken");
        String input=in.text("csv",200000,true);
        if(input.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>200000)throw CatalogInput.bad("CSV tối đa 200 KB");
        var parsed=parse(input);
        if(parsed.isEmpty()||!parsed.get(0).equals(List.of("sku","name","price","categoryId","imageUrl","description")))throw CatalogInput.bad("Header cần: sku,name,price,categoryId,imageUrl,description");
        var rows=new ArrayList<Map<String,Object>>();
        var drafts=new ArrayList<ObjectNode>();
        var existing=new ArrayList<Long>();
        Set<String> skus=new HashSet<>();
        boolean valid=true;
        for(int i=1;i<parsed.size();i++) {
            var cells=parsed.get(i);
            try {
                if(cells.size()!=6)throw CatalogInput.bad("Cần 6 cột");
                for(String c:cells) {
                    if(c.startsWith("\t")||c.startsWith("\r"))throw CatalogInput.bad("Không chấp nhận ô công thức");
                    String v=c.stripLeading();
                    if(!v.isEmpty()&&"=+-@\t\r".indexOf(v.charAt(0))>=0)throw CatalogInput.bad("Không chấp nhận ô công thức");
                }
                String sku=cells.get(0).strip();
                if(!skus.add(sku.toLowerCase(Locale.ROOT)))throw CatalogInput.bad("SKU trùng trong file");
                var d=mapper.createObjectNode();
                d.put("sku",sku);
                d.put("name",cells.get(1));
                d.put("price",new BigDecimal(cells.get(2)));
                d.put("categoryId",Long.parseLong(cells.get(3)));
                d.put("imageUrl",cells.get(4));
                d.put("description",cells.get(5));
                var check=new CatalogInput(d);
                check.text("sku",100,true);
                check.text("name",255,true);
                check.text("description",4000,false);
                check.money("price",false);
                catalog.required(ProductCategory.class,check.id("categoryId",true));
                images.check(check.text("imageUrl",500,false));
                var matches=em.createQuery("from Product p where lower(p.sku)=:sku",Product.class).setParameter("sku",sku.toLowerCase(Locale.ROOT)).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
                if(matches.size()>1)throw CatalogInput.bad("SKU hiện tại không duy nhất");
                Long id=matches.isEmpty()?null:matches.get(0).getId();
                d.put("active",id==null||matches.get(0).getActive());
                if(id!=null) {
                    var stock=em.createQuery("from Inventory i where i.product.id=:id",Inventory.class).setParameter("id",id).getResultList();
                    if(!stock.isEmpty()&&stock.get(0).getReorderPoint()!=null)d.put("reorderPoint",stock.get(0).getReorderPoint());
                }
                drafts.add(d);
                existing.add(id);
                rows.add(Map.of("id",i,"sku",sku,"action",id==null?"CREATE":"UPDATE","valid",true,"message","Hợp lệ","snapshot",id==null?"new":matches.get(0).getUpdatedAt().toString()));
            }
            catch(Exception e) {
                valid=false;
                rows.add(Map.of("id",i,"valid",false,"message",e instanceof com.petcarehub.exception.BadRequestException||e instanceof com.petcarehub.exception.ResourceNotFoundException?e.getMessage():"Số hoặc định dạng không hợp lệ"));
            }
        }
        String token=CatalogInput.fingerprint(input+rows.toString());
        if(apply) {
            if(!valid)throw CatalogInput.bad("File còn lỗi; chưa nhập dòng nào");
            if(!token.equals(in.text("previewToken",64,true)))throw new IllegalStateException("Dữ liệu thay đổi, xem trước lại");
            for(int i=0;i<drafts.size();i++)catalog.saveInternal("products",existing.get(i),new CatalogInput(drafts.get(i)));
            em.flush();
        }
        audit.record(apply?"PRODUCT_CSV_APPLIED":"PRODUCT_CSV_PREVIEW","PRODUCT",null,Map.of("count",rows.size()));
        return Map.of("rows",rows,"valid",valid,"previewToken",token);
    }
    static List<List<String>> parse(String text) {
        if(text.startsWith("\uFEFF"))text=text.substring(1);
        List<List<String>> rows=new ArrayList<>();
        List<String> row=new ArrayList<>();
        StringBuilder cell=new StringBuilder();
        boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='"') {
                if(quoted&&i+1<text.length()&&text.charAt(i+1)=='"') {
                    cell.append('"');
                    i++;
                }
                else if(quoted) {
                    quoted=false;
                    closed=true;
                }
                else if(cell.isEmpty()&&!closed)quoted=true;
                else throw CatalogInput.bad("Dấu ngoặc kép CSV không hợp lệ");
            }
            else if(!quoted&&(c==','||c=='\n'||c=='\r')) {
                row.add(cell.toString());
                cell.setLength(0);
                closed=false;
                if(c!=',') {
                    if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;
                    if(!(row.size()==1&&row.get(0).isBlank()))rows.add(row);
                    row=new ArrayList<>();
                    if(rows.size()>201)throw CatalogInput.bad("Tối đa 200 dòng sản phẩm");
                }
            }
            else {
                if(closed)throw CatalogInput.bad("Ký tự sau ô CSV đã đóng");
                cell.append(c);
            }
        }
        if(quoted)throw CatalogInput.bad("Ô CSV chưa đóng");
        if(!cell.isEmpty()||!row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        if(rows.size()<2||rows.size()>201)throw CatalogInput.bad("Cần từ 1 đến 200 dòng sản phẩm");
        return rows;
    }
}
