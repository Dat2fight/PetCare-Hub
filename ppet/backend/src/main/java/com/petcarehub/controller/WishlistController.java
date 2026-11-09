package com.petcarehub.controller;

import com.petcarehub.entity.*;
import com.petcarehub.repository.*;
import com.petcarehub.enums.AvailabilityStatus;
import com.petcarehub.exception.*;
import com.petcarehub.security.Ownership;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController @RequestMapping("/api/v1/wishlist")
@PreAuthorize("hasRole('CUSTOMER')") @RequiredArgsConstructor
@Transactional(readOnly=true)
public class WishlistController {
    private final Ownership ownership;
    private final UserRepository users;
    private final WishlistRepository items;
    private final ProductRepository products;
    private final PetRepository pets;
    private final InventoryRepository inventory;

    public record Target(@NotNull @Pattern(regexp="PRODUCT|PET") String type, @NotNull @Positive Long targetId) {}
    public record Match(Long id, Long targetId) {}
    public record Item(Long id, String type, Long targetId, String name, BigDecimal price, String imageUrl, boolean available) {}

    @GetMapping
    public Page<Item> list(Pageable page) {
        var result = items.findByCustomerIdOrderByIdDesc(ownership.userId(), page);
        var ids = result.stream().filter(w -> w.getProduct()!=null).map(w -> w.getProduct().getId()).toList();
        var inStock = inventory.findByProductIdIn(ids).stream().filter(i -> i.getQuantity()>0)
                .map(i -> i.getProduct().getId()).collect(Collectors.toSet());
        return result.map(w -> {
            var p = w.getProduct();
            if (p!=null) return new Item(w.getId(),"PRODUCT",p.getId(),p.getName(),p.getPrice(),p.getImageUrl(),
                    Boolean.TRUE.equals(p.getActive()) && inStock.contains(p.getId()));
            var pet = w.getPet();
            if (pet.getOwner()!=null) return new Item(w.getId(),"PET",pet.getId(),"Thú cưng đã ngừng bán",null,null,false);
            return new Item(w.getId(),"PET",pet.getId(),pet.getName(),pet.getPrice(),pet.getImageUrl(),available(pet));
        });
    }

    private boolean available(Pet pet) {
        return pet.getOwner()==null && pet.getAvailabilityStatus()==AvailabilityStatus.AVAILABLE && pet.getPrice().signum()>0;
    }
    private Match match(WishlistItem w) {
        return new Match(w.getId(),w.getProduct()!=null ? w.getProduct().getId() : w.getPet().getId());
    }

    @GetMapping("/status")
    public List<Match> status(@RequestParam String type, @RequestParam List<Long> ids) {
        if (ids.size()>100 || !Set.of("PRODUCT","PET").contains(type)) throw new BadRequestException("Danh sách không hợp lệ");
        return items.matches(ownership.userId(),type,ids).stream().map(this::match).toList();
    }

    @PostMapping @Transactional
    public Match save(@Valid @RequestBody Target target) {
        var customer=users.findLockedById(ownership.userId()).orElseThrow();
        var existing=items.matches(customer.getId(),target.type(),List.of(target.targetId()));
        if (!existing.isEmpty()) return match(existing.get(0));
        var item=new WishlistItem();
        item.setCustomer(customer);
        item.setType(target.type());
        if ("PRODUCT".equals(target.type())) item.setProduct(products.findById(target.targetId())
                .filter(p->Boolean.TRUE.equals(p.getActive())).orElseThrow(this::missing));
        else item.setPet(pets.findById(target.targetId()).filter(this::available).orElseThrow(this::missing));
        return match(items.save(item));
    }

    @DeleteMapping("/{id}") @Transactional @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id) {
        var user=users.findLockedById(ownership.userId()).orElseThrow();
        items.delete(items.findByIdAndCustomerId(id,user.getId()).orElseThrow(this::missing));
    }
    private ResourceNotFoundException missing() { return new ResourceNotFoundException("Không tìm thấy mục yêu thích"); }
}
