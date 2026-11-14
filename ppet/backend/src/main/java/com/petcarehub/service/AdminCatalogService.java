package com.petcarehub.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.exception.*;
import com.petcarehub.dto.product.*;
import com.petcarehub.dto.pet.*;
import com.petcarehub.security.*;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class AdminCatalogService  {
    private final EntityManager em;
    private final AdminQuery query;
    private final AdminAuditService audit;
    private final CatalogMutex mutex;
    private final CatalogImagePolicy images;
    private final ProductService products;
    private final PetService pets;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final OrderService orders;
    private final Ownership ownership;
    record View(Class<?> type,String from,String columns,String names,String where,String search,Map<String,String> sorts) {
    }
    private View view(String resource) {
        return switch(resource) {
            case "products"->new View(Product.class,"Product o join o.category c left join Inventory i on i.product=o","o.id,o.name,o.description,o.price,o.imageUrl,c.id,c.name,o.sku,o.active,i.quantity,i.reorderPoint","id,name,description,price,imageUrl,categoryId,categoryName,sku,active,quantity,reorderPoint","1=1","o.name",Map.of("id","o.id","name","o.name","price","o.price"));
            case "categories"->new View(ProductCategory.class,"ProductCategory o left join o.parentCategory p","o.id,o.name,o.description,p.id,o.displayOrder","id,name,description,parentId,displayOrder","1=1","o.name",Map.of("id","o.id","name","o.name","displayOrder","o.displayOrder"));
            case "species"->new View(Species.class,"Species o","o.id,o.name,o.description","id,name,description","1=1","o.name",Map.of("id","o.id","name","o.name"));
            case "breeds"->new View(Breed.class,"Breed o join o.species s","o.id,o.name,o.description,s.id,s.name","id,name,description,speciesId,speciesName","1=1","o.name",Map.of("id","o.id","name","o.name"));
            case "pets"->new View(Pet.class,"Pet o join o.species s left join o.breed b left join o.reservedOrder r left join r.customer u","o.id,o.name,o.description,o.price,o.imageUrl,s.id,b.id,o.dateOfBirth,o.healthStatus,o.availabilityStatus,r.id,r.orderNumber,r.expiresAt,u.id","id,name,description,price,imageUrl,speciesId,breedId,birthDate,healthStatus,availabilityStatus,orderId,orderNumber,expiresAt,customerId","o.owner is null","o.name",Map.of("id","o.id","name","o.name","price","o.price"));
            case "services"->new View(ServiceEntity.class,"ServiceEntity o","o.id,o.name,o.description,o.price,o.durationMinutes,o.serviceType,o.active","id,name,description,price,durationMinutes,serviceType,active","1=1","o.name",Map.of("id","o.id","name","o.name","price","o.price"));
            case "promotions"->new View(Promotion.class,"Promotion o","o.id,o.code,o.description,o.discountType,o.discountValue,o.minOrderAmount,o.maxUses,o.maxUsesPerCustomer,o.currentUses,o.startDate,o.endDate,o.active","id,code,description,discountType,discountValue,minOrderAmount,maxUses,maxUsesPerCustomer,currentUses,startDate,endDate,active","1=1","o.code",Map.of("id","o.id","code","o.code"));
            case "packages"->new View(CarePackage.class,"CarePackage o","o.id,o.name,o.description,o.price,o.durationMonths,o.includesGrooming,o.includesCheckup,o.includesVaccination,o.active","id,name,description,price,durationMonths,includesGrooming,includesCheckup,includesVaccination,active","1=1","o.name",Map.of("id","o.id","name","o.name","price","o.price"));
            default->throw new ResourceNotFoundException("Không tìm thấy danh mục quản trị");
        }
        ;
    }
    @PreAuthorize("@catalogPolicy.read(#resource)") @Transactional(readOnly=true)
    public Page<Map<String,Object>> list(String resource,String keyword,Boolean active,Pageable page) {
        var v=view(resource);
        String where=v.where();
        Map<String,Object> p=new HashMap<>();
        if(keyword!=null&&!keyword.isBlank()) {
            where+=" and lower("+v.search()+") like :keyword escape '!'";
            p.put("keyword",AdminQuery.pattern(keyword));
        }
        if(active!=null&&Set.of("products","services","promotions","packages").contains(resource)) {
            where+=" and o.active=:active";
            p.put("active",active);
        }
        return query.page(v.from(),v.columns(),v.names(),where,p,page,v.sorts());
    }
    @PreAuthorize("@catalogPolicy.read(#resource)") @Transactional(readOnly=true)
    public Map<String,Object> detail(String resource,Long id) {
        return detailRow(resource,id);
    }
    private Map<String,Object> detailRow(String resource,Long id) {
        var v=view(resource);
        return query.page(v.from(),v.columns(),v.names(),v.where()+" and o.id=:id",Map.of("id",id),PageRequest.of(0,1),v.sorts()).stream().findFirst().orElseThrow(()->new ResourceNotFoundException("Không tìm thấy dữ liệu"));
    }
    @PreAuthorize("@catalogPolicy.read('products')") @Transactional(readOnly=true)
    public Map<String,Object> lowStock() {
        var row=em.createQuery("select count(p),coalesce(sum(i.quantity),0) from Product p join Inventory i on i.product=p where p.active=true and i.quantity<=coalesce(i.reorderPoint,10)",Object[].class).getSingleResult();
        return Map.of("count",row[0],"quantity",row[1]);
    }
    public <T>T required(Class<T> type,Long id) {
        if(id==null)throw CatalogInput.bad("Thiếu ID");
        var value=em.find(type,id);
        if(value==null)throw new ResourceNotFoundException("Không tìm thấy dữ liệu tham chiếu #"+id);
        return value;
    }
    public long count(String from,String where,Long id) {
        return em.createQuery("select count(o) from "+from+" o where "+where,Long.class).setParameter("id",id).getSingleResult();
    }
    private boolean petUsed(Long id) {
        return count("OrderItem","o.pet.id=:id",id)>0;
    }
    private void editablePet(Pet pet) {
        if(pet.getOwner()!=null||pet.getReservedOrder()!=null||petUsed(pet.getId()))throw new IllegalStateException("Thú cưng đã có chủ hoặc gắn đơn; không được sửa/xóa trực tiếp");
    }
    private <T>T existing(Class<T> type,Long id,java.util.function.Supplier<T> factory) {
        return id==null?factory.get():em.find(type,id,LockModeType.PESSIMISTIC_WRITE);
    }
    @PreAuthorize("@catalogPolicy.write(#resource)")
    public Map<String,Object> save(String resource,Long id,JsonNode body) {
        mutex.write();
        Long saved=saveInternal(resource,id,new CatalogInput(body));
        em.flush();
        audit.record("CATALOG_"+(id==null?"CREATED":"UPDATED"),resource.toUpperCase(Locale.ROOT),saved,Map.of());
        return detailRow(resource,saved);
    }
    Long saveInternal(String resource,Long id,CatalogInput in) {
        if(id!=null)required(view(resource).type(),id);
        String description=in.text("description",4000,false);
        switch(resource) {
            case "products": {
                in.keys("name,description,price,imageUrl,categoryId,active,sku,reorderPoint");
                var category=required(ProductCategory.class,in.id("categoryId",true));
                String name=in.text("name",255,true),sku=in.text("sku",100,true),url=images.check(in.text("imageUrl",500,false));
                var price=in.money("price",false);
                if(em.createQuery("select count(p) from Product p where lower(p.sku)=:sku and (:id is null or p.id<>:id)",Long.class).setParameter("sku",sku.toLowerCase(Locale.ROOT)).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("SKU đã tồn tại");
                ProductDTO dto;
                if(id==null) {
                    var r=new ProductCreateRequest(name,description,price,null,category.getId(),in.bool("active",true),sku,url);
                    dto=products.createProduct(r);
                }
                else {
                    em.find(Product.class,id,LockModeType.PESSIMISTIC_WRITE);
                    var r=new ProductUpdateRequest();
                    r.setName(name);
                    r.setDescription(description==null?"":description);
                    r.setPrice(price);
                    r.setImageUrl(url==null?"":url);
                    r.setSku(sku);
                    r.setActive(in.bool("active",true));
                    r.setCategoryId(category.getId());
                    dto=products.updateProduct(id,r);
                }
                var inventory=em.createQuery("from Inventory where product.id=:id",Inventory.class).setParameter("id",dto.getId()).setLockMode(LockModeType.PESSIMISTIC_WRITE).getSingleResult();
                inventory.setReorderPoint(in.json().hasNonNull("reorderPoint")?in.integer("reorderPoint",0,1000000,10):null);
                return dto.getId();
            }
            case "categories": {
                in.keys("name,description,parentId,displayOrder");
                var c=existing(ProductCategory.class,id,ProductCategory::new);
                Long parent=in.id("parentId",false);
                var p=parent==null?null:required(ProductCategory.class,parent);
                Set<Long> seen=new HashSet<>();
                for(var cursor=p;cursor!=null;cursor=cursor.getParentCategory()) {
                    if(Objects.equals(cursor.getId(),id)||!seen.add(cursor.getId()))throw CatalogInput.bad("Danh mục không được tạo vòng lặp");
                }
                c.setName(in.text("name",100,true));
                c.setDescription(description);
                c.setParentCategory(p);
                c.setDisplayOrder(in.integer("displayOrder",0,1000000,0));
                if(id==null)em.persist(c);
                return c.getId();
            }
            case "species": {
                in.keys("name,description");
                var s=existing(Species.class,id,Species::new);
                s.setName(in.text("name",100,true));
                s.setDescription(description);
                if(id==null)em.persist(s);
                return s.getId();
            }
            case "breeds": {
                in.keys("name,description,speciesId");
                var b=existing(Breed.class,id,Breed::new);
                var species=required(Species.class,in.id("speciesId",true));
                if(id!=null&&count("Pet","o.breed.id=:id",id)>0&&!b.getSpecies().getId().equals(species.getId()))throw new IllegalStateException("Giống đang được dùng, không đổi loài");
                b.setName(in.text("name",100,true));
                b.setDescription(description);
                b.setSpecies(species);
                if(id==null)em.persist(b);
                return b.getId();
            }
            case "pets": {
                in.keys("name,description,price,imageUrl,speciesId,breedId,birthDate,healthStatus,availabilityStatus");
                if(id!=null)editablePet(em.find(Pet.class,id,LockModeType.PESSIMISTIC_WRITE));
                var date=in.date("birthDate",false);
                if(date!=null&&date.isAfter(LocalDate.now(clock)))throw CatalogInput.bad("Ngày sinh trong tương lai");
                String status=in.text("availabilityStatus",50,true);
                if(!Set.of("AVAILABLE","UNAVAILABLE").contains(status))throw CatalogInput.bad("Chỉ chọn đang bán hoặc ngừng bán; giữ chỗ/bán xong do đơn hàng xử lý");
                in.choice("healthStatus",HealthStatus.class);
                in.money("price",true);
                images.check(in.text("imageUrl",500,false));
                in.text("name",100,true);
                required(Species.class,in.id("speciesId",true));
                Long breed=in.id("breedId",false);
                if(breed!=null&&!required(Breed.class,breed).getSpecies().getId().equals(in.id("speciesId",true)))throw CatalogInput.bad("Giống không thuộc loài");
                if(id==null)return pets.createPet(mapper.convertValue(in.json(),PetCreateRequest.class)).getId();
                return pets.updatePet(id,mapper.convertValue(in.json(),PetUpdateRequest.class)).getId();
            }
            case "services": {
                in.keys("name,description,price,durationMinutes,serviceType,active");
                var s=existing(ServiceEntity.class,id,ServiceEntity::new);
                var type=in.choice("serviceType",ServiceType.class);
                if(id!=null&&s.getServiceType()!=type&&count("Appointment","o.service.id=:id",id)>0)throw new IllegalStateException("Dịch vụ có lịch; không đổi loại nhân viên");
                s.setName(in.text("name",100,true));
                s.setDescription(description);
                s.setPrice(in.money("price",false));
                s.setDurationMinutes(in.integer("durationMinutes",5,480,-1));
                if(s.getDurationMinutes()<5)throw CatalogInput.bad("Thời lượng bắt buộc");
                s.setServiceType(type);
                s.setActive(in.bool("active",true));
                if(id==null)em.persist(s);
                return s.getId();
            }
            case "promotions": {
                in.keys("code,description,discountType,discountValue,minOrderAmount,maxUses,maxUsesPerCustomer,startDate,endDate,active");
                var p=existing(Promotion.class,id,Promotion::new);
                String code=in.text("code",50,true).toUpperCase(Locale.ROOT);
                if(!code.matches("[A-Z0-9_-]{2,50}"))throw CatalogInput.bad("Mã chỉ gồm chữ, số, gạch dưới/ngang");
                if(em.createQuery("select count(p) from Promotion p where upper(p.code)=:code and (:id is null or p.id<>:id)",Long.class).setParameter("code",code).setParameter("id",id).getSingleResult()>0)throw new IllegalStateException("Mã đã tồn tại");
                var type=in.choice("discountType",DiscountType.class);
                var value=in.money("discountValue",true);
                if(type==DiscountType.PERCENTAGE&&(value.compareTo(java.math.BigDecimal.ONE)<0||value.compareTo(java.math.BigDecimal.valueOf(100))>0))throw CatalogInput.bad("Phần trăm từ 1 đến 100");
                var minimum=in.money("minOrderAmount",false);
                var from=in.dateTime("startDate");
                var to=in.dateTime("endDate");
                if(!from.isBefore(to))throw CatalogInput.bad("Thời hạn phải từ trước đến sau");
                Integer max=in.json().hasNonNull("maxUses")?in.integer("maxUses",1,1000000,1):null;
                int per=in.integer("maxUsesPerCustomer",1,1000000,1);
                boolean active=in.bool("active",true);
                boolean used=id!=null&&(p.getCurrentUses()>0||count("Order","upper(o.promotionCode)=upper((select p.code from Promotion p where p.id=:id))",id)>0);
                if(used&&(!p.getCode().equalsIgnoreCase(code)||p.getDiscountType()!=type||p.getDiscountValue().compareTo(value)!=0||(p.getMinOrderAmount()==null?minimum.signum()!=0:p.getMinOrderAmount().compareTo(minimum)!=0)||!Objects.equals(p.getMaxUses(),max)||!Objects.equals(p.getMaxUsesPerCustomer(),per)||!Objects.equals(p.getStartDate(),from)||p.getEndDate()!=null&&to.isBefore(p.getEndDate())||!Boolean.TRUE.equals(p.getActive())&&active))throw new IllegalStateException("Mã đã dùng chỉ được gia hạn hoặc tắt");
                p.setCode(code);
                p.setDescription(description);
                p.setDiscountType(type);
                p.setDiscountValue(value);
                p.setMinOrderAmount(minimum);
                p.setMaxUses(max);
                p.setMaxUsesPerCustomer(per);
                p.setStartDate(from);
                p.setEndDate(to);
                p.setActive(active);
                if(id==null)em.persist(p);
                return p.getId();
            }
            case "packages": {
                in.keys("name,description,price,durationMonths,includesGrooming,includesCheckup,includesVaccination,active");
                var p=existing(CarePackage.class,id,CarePackage::new);
                p.setName(in.text("name",255,true));
                p.setDescription(description);
                p.setPrice(in.money("price",true));
                p.setDurationMonths(in.integer("durationMonths",1,120,1));
                p.setIncludesGrooming(in.bool("includesGrooming",false));
                p.setIncludesCheckup(in.bool("includesCheckup",false));
                p.setIncludesVaccination(in.bool("includesVaccination",false));
                p.setActive(in.bool("active",true));
                if(id==null)em.persist(p);
                return p.getId();
            }
            default:throw CatalogInput.bad("Danh mục không hợp lệ");
        }
    }
    @PreAuthorize("@catalogPolicy.write(#resource)")
    public void delete(String resource,Long id,String reason) {
        if(reason==null||reason.isBlank())throw CatalogInput.bad("Cần lý do xóa");
        mutex.write();
        var entity=required(view(resource).type(),id);
        em.lock(entity,LockModeType.PESSIMISTIC_WRITE);
        boolean used=switch(resource) {
            case "products"->count("OrderItem","o.product.id=:id",id)>0||count("InventoryTransaction","o.inventory.product.id=:id",id)>0||count("CartItem","o.product.id=:id",id)>0;
            case "categories"->count("Product","o.category.id=:id",id)>0||count("ProductCategory","o.parentCategory.id=:id",id)>0;
            case "species"->count("Pet","o.species.id=:id",id)>0||count("Breed","o.species.id=:id",id)>0;
            case "breeds"->count("Pet","o.breed.id=:id",id)>0;
            case "pets"-> {
                editablePet((Pet)entity);
                yield count("Appointment","o.pet.id=:id",id)>0;
            }
            case "services"->count("Appointment","o.service.id=:id",id)>0;
            case "promotions"->((Promotion)entity).isRestrictedAudience()||count("PromotionUsage","o.promotion.id=:id",id)>0||count("Order","upper(o.promotionCode)=upper((select p.code from Promotion p where p.id=:id))",id)>0;
            case "packages"->count("CarePackageSubscription","o.carePackage.id=:id",id)>0||count("OrderItem","o.carePackage.id=:id",id)>0;
            default->true;
        }
        ;
        if(used)throw new IllegalStateException("Dữ liệu đã được tham chiếu; hãy ngừng hoạt động thay vì xóa");
        if(resource.equals("products")) {
            var inventory=em.createQuery("from Inventory where product.id=:id",Inventory.class).setParameter("id",id).getResultList();
            for(var item:inventory) {
                if(item.getQuantity()!=0)throw new IllegalStateException("Sản phẩm còn tồn kho");
                em.remove(item);
            }
        }
        em.remove(entity);
        em.flush();
        audit.record("CATALOG_DELETED",resource.toUpperCase(Locale.ROOT),id,Map.of("reasonProvided",true));
    }
    @PreAuthorize("@perm.has('SALE_PET_MANAGE')")
    public void release(Long id,String reason) {
        if(reason==null||reason.isBlank())throw CatalogInput.bad("Cần lý do nhả giữ chỗ");
        var snapshot=required(Pet.class,id);
        if(snapshot.getReservedOrder()==null)throw new IllegalStateException("Thú cưng không có đơn giữ chỗ");
        Long orderId=snapshot.getReservedOrder().getId();
        em.find(Order.class,orderId,LockModeType.PESSIMISTIC_WRITE);
        var pet=em.find(Pet.class,id,LockModeType.PESSIMISTIC_WRITE);
        em.refresh(pet);
        if(pet.getReservedOrder()==null||!pet.getReservedOrder().getId().equals(orderId))throw new IllegalStateException("Giữ chỗ đã thay đổi");
        orders.cancelOrder(orderId);
        em.persist(OrderNote.builder().orderId(orderId).actorId(ownership.userId()).body(reason).createdAt(LocalDateTime.now(clock)).build());
        em.persist(Notification.builder().user(required(Order.class,orderId).getCustomer()).type(NotificationType.ORDER_UPDATE).title("Đơn giữ chỗ đã hủy").message("Đơn #"+orderId+" đã được cửa hàng hủy toàn bộ và nhả giữ chỗ.").build());
        audit.record("PET_RESERVATION_RELEASED","PET",id,Map.of("reasonProvided",true));
    }
    @PreAuthorize("@perm.has('USER_MANAGE') or @perm.has('ASSIGNED_MEDICAL_MANAGE')")
    public Map<String,Object> support(Long id) {
        var pet=required(Pet.class,id);
        if(pet.getOwner()==null||!ownership.hasRole("ADMIN")&&em.createQuery("select count(a) from Appointment a where a.pet.id=:id and a.staff.id=:staff",Long.class).setParameter("id",id).setParameter("staff",ownership.userId()).getSingleResult()==0)throw new ResourceNotFoundException("Không tìm thấy thú cưng");
        audit.record("CUSTOMER_PET_READ","PET",id,Map.of());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",id);
        result.put("name",pet.getName());
        result.put("birthDate",pet.getDateOfBirth());
        result.put("species",pet.getSpecies().getName());
        result.put("healthStatus",pet.getHealthStatus());
        result.put("ownerId",pet.getOwner().getId());
        return result;
    }
}
