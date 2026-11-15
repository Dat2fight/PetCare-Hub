package com.petcarehub.service;
import com.petcarehub.entity.*;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.security.Ownership;
import com.fasterxml.jackson.databind.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class SiteContentService {
    private final EntityManager em;
    private final AdminQuery query;
    private final CatalogMutex mutex;
    private final AdminAuditService audit;
    private final Ownership ownership;
    private final Clock clock;
    private final ObjectMapper json;
    public static final Set<String> PAGES=Set.of("home","contact","terms","privacy","shipping-policy","return-policy","cancellation-policy");
    private SiteContent find(long id){
        var c=em.find(SiteContent.class,id);
        if(c==null)throw new ResourceNotFoundException("Không tìm thấy nội dung");
        return c;
    }
    private String encode(Object value){
        try{
            return json.writeValueAsString(value);
        }
        catch(Exception e){
            throw new IllegalStateException("Không thể lưu nội dung");
        }
    }
    private JsonNode decode(String value){
        try{
            return json.readTree(value);
        }
        catch(Exception e){
            throw new IllegalStateException("Nội dung lưu không hợp lệ");
        }
    }
    private String snapshot(SiteContent c){
        return encode(Map.of("title",c.getTitle(),"body",c.getBody(),"configuration",decode(c.getConfiguration())));
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(String kind,String keyword,Pageable p){
        String where="1=1";
        Map<String,Object> params=new HashMap<>();
        if(kind!=null&&!kind.isBlank()){
            where+=" and o.kind=:kind";
            params.put("kind",kind);
        }
        if(keyword!=null&&!keyword.isBlank()){
            where+=" and lower(o.title) like :word escape '!'";
            params.put("word",AdminQuery.pattern(keyword));
        }
        return query.page("SiteContent o","o.id,o.slug,o.kind,o.title,o.publishedAt,o.updatedAt","id,slug,kind,title,publishedAt,updatedAt",where,params,p,Map.of("id","o.id","title","o.title"));
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") @Transactional(readOnly=true)
    public Object detail(long id){
        var c=find(id);
        return Map.of("id",c.getId(),"slug",c.getSlug(),"kind",c.getKind(),"title",c.getTitle(),"body",c.getBody(),"configuration",decode(c.getConfiguration()),"published",c.getPublishedSnapshot()!=null);
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')")
    public Object save(Long id,JsonNode body){
        mutex.write();
        var in=new CatalogInput(body);
        in.keys("slug,kind,title,body,configuration");
        String slug=in.text("slug",100,true).toLowerCase(Locale.ROOT),kind=in.text("kind",20,true);
        if(!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")||!Set.of("PAGE","ARTICLE","FAQ").contains(kind)||kind.equals("PAGE")!=PAGES.contains(slug))throw CatalogInput.bad("Loại/slug không hợp lệ; trang cố định dùng mã được hỗ trợ");
        var c=id==null?new SiteContent():find(id);
        if(id!=null&&(!c.getSlug().equals(slug)||!c.getKind().equals(kind)))throw CatalogInput.bad("Không đổi slug/loại của nội dung đã tạo");
        if(em.createQuery("select count(c) from SiteContent c where c.slug=:slug and (:id is null or c.id<>:id)",Long.class).setParameter("slug",slug).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("Slug đã tồn tại");
        c.setSlug(slug);
        c.setKind(kind);
        c.setTitle(in.text("title",200,true));
        c.setBody(in.text("body",20000,true));
        var config=body.path("configuration");
        if(config.isMissingNode())config=json.createObjectNode();
        validateConfig(slug,config);
        c.setConfiguration(encode(config));
        c.setUpdatedAt(LocalDateTime.now(clock));
        c.setUpdatedBy(ownership.userId());
        if(id==null)em.persist(c);
        revision(c);
        audit.record("CONTENT_DRAFT_SAVED","CONTENT",c.getId(),Map.of());
        return Map.of("id",c.getId());
    }
    private void validateConfig(String slug,JsonNode config){
        var in=new CatalogInput(config);
        in.keys(slug.equals("home")?"productIds,petIds":"");
        for(String key:List.of("productIds","petIds"))if(config.has(key)){
            var ids=config.get(key);
            if(!ids.isArray()||ids.size()>12)throw CatalogInput.bad("Tối đa 12 ID mỗi danh sách");
            Set<Long> seen=new HashSet<>();
            for(var n:ids){
                if(!n.isIntegralNumber()||!n.canConvertToLong()||n.longValue()<1||!seen.add(n.longValue()))throw CatalogInput.bad("ID nổi bật không hợp lệ/trùng");
                Long count=em.createQuery(key.equals("productIds")?"select count(p) from Product p where p.id=:id and p.active=true":"select count(p) from Pet p where p.id=:id and p.owner is null and p.availabilityStatus=com.petcarehub.enums.AvailabilityStatus.AVAILABLE",Long.class).setParameter("id",n.longValue()).getSingleResult();
                if(count==0)throw CatalogInput.bad("ID nổi bật không tồn tại hoặc không đang bán");
            }
        }
    }
    private void revision(SiteContent c){
        var r=new ContentRevision();
        r.setContentId(c.getId());
        r.setSnapshot(snapshot(c));
        r.setActorId(ownership.userId());
        r.setCreatedAt(LocalDateTime.now(clock));
        em.persist(r);
        em.flush();
        var old=em.createQuery("select r.id from ContentRevision r where r.contentId=:id order by r.id desc",Long.class).setParameter("id",c.getId()).setFirstResult(20).setMaxResults(100).getResultList();
        if(!old.isEmpty())em.createQuery("delete from ContentRevision where id in :ids").setParameter("ids",old).executeUpdate();
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void publish(long id,boolean publish){
        mutex.write();
        var c=find(id);
        if(publish){
            validateConfig(c.getSlug(),decode(c.getConfiguration()));
            c.setPublishedSnapshot(snapshot(c));
            c.setPublishedAt(LocalDateTime.now(clock));
        }
        else{
            c.setPublishedSnapshot(null);
            c.setPublishedAt(null);
        }
        audit.record(publish?"CONTENT_PUBLISHED":"CONTENT_UNPUBLISHED","CONTENT",id,Map.of());
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") @Transactional(readOnly=true) public Object revisions(long id,Pageable p){
        find(id);
        return query.page("ContentRevision o","o.id,o.snapshot,o.actorId,o.createdAt","id,snapshot,actorId,createdAt","o.contentId=:id",Map.of("id",id),p,Map.of("id","o.id"));
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void restore(long id,long revision){
        mutex.write();
        var c=find(id);
        var r=em.find(ContentRevision.class,revision);
        if(r==null||!r.getContentId().equals(id))throw new ResourceNotFoundException("Không tìm thấy phiên bản");
        var value=decode(r.getSnapshot());
        c.setTitle(value.get("title").asText());
        c.setBody(value.get("body").asText());
        c.setConfiguration(encode(value.get("configuration")));
        c.setUpdatedAt(LocalDateTime.now(clock));
        c.setUpdatedBy(ownership.userId());
        revision(c);
        audit.record("CONTENT_RESTORED","CONTENT",id,Map.of());
    }
    @PreAuthorize("@perm.has('CONTENT_MANAGE')") public void delete(long id,String reason){
        if(reason==null||reason.isBlank())throw CatalogInput.bad("Cần lý do");
        mutex.write();
        var c=find(id);
        if(c.getPublishedSnapshot()!=null)throw new IllegalStateException("Hủy xuất bản trước khi xóa");
        em.createQuery("delete from ContentRevision where contentId=:id").setParameter("id",id).executeUpdate();
        em.remove(c);
        audit.record("CONTENT_DELETED","CONTENT",id,Map.of("reasonProvided",true));
    }
    @Transactional(readOnly=true) public Object publicDetail(String slug){
        var rows=em.createQuery("from SiteContent c where c.slug=:slug and c.publishedSnapshot is not null",SiteContent.class).setParameter("slug",slug).setMaxResults(1).getResultList();
        if(rows.isEmpty())throw new ResourceNotFoundException("Nội dung chưa xuất bản");
        var c=rows.get(0);
        return Map.of("slug",c.getSlug(),"kind",c.getKind(),"content",decode(c.getPublishedSnapshot()),"publishedAt",c.getPublishedAt());
    }
    @Transactional(readOnly=true) public Object publicList(String kind,Pageable p){
        if(!Set.of("ARTICLE","FAQ").contains(kind))throw CatalogInput.bad("Chọn ARTICLE hoặc FAQ");
        return query.page("SiteContent o","o.id,o.slug,o.publishedSnapshot,o.publishedAt","id,slug,snapshot,publishedAt","o.kind=:kind and o.publishedSnapshot is not null",Map.of("kind",kind),p,Map.of("id","o.id"));
    }
    @Transactional(readOnly=true) public Object featured(){
        var snapshots=em.createQuery("select c.publishedSnapshot from SiteContent c where c.slug='home' and c.publishedSnapshot is not null",String.class).setMaxResults(1).getResultList();
        Map<String,Object> result=new HashMap<>();
        if(snapshots.isEmpty())return result;
        var config=decode(snapshots.get(0)).path("configuration");
        for(String key:List.of("productIds","petIds"))if(config.has(key)){
            List<Long> ids=new ArrayList<>();
            config.get(key).forEach(n->ids.add(n.longValue()));
            List<Map<String,Object>> items=new ArrayList<>();
            if(!ids.isEmpty()){
                String hql=key.equals("productIds")?"select p.id,p.name,p.imageUrl,p.price from Product p where p.id in :ids and p.active=true":"select p.id,p.name,p.imageUrl,p.price from Pet p where p.id in :ids and p.owner is null and p.availabilityStatus=com.petcarehub.enums.AvailabilityStatus.AVAILABLE";
                for(var r:em.createQuery(hql,Object[].class).setParameter("ids",ids).setMaxResults(12).getResultList()){
                    Map<String,Object> item=new HashMap<>();
                    item.put("id",r[0]);
                    item.put("name",r[1]);
                    item.put("imageUrl",r[2]);
                    item.put("price",r[3]);
                    items.add(item);
                }
                items.sort(Comparator.comparingInt(item->ids.indexOf(item.get("id"))));
            }
            result.put(key.equals("productIds")?"products":"pets",items);
        }
        return result;
    }
}
