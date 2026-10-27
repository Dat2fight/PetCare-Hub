package com.petcarehub.service.impl;

import com.petcarehub.dto.order.*;
import com.petcarehub.dto.inventory.StockUpdateRequest;
import com.petcarehub.entity.*;
import com.petcarehub.enums.*;
import com.petcarehub.exception.*;
import com.petcarehub.repository.*;
import com.petcarehub.security.Ownership;
import com.petcarehub.service.*;
import com.petcarehub.state.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final PetRepository pets;
    private final UserRepository users;
    private final InventoryService inventory;
    private final Ownership ownership;
    private final CartRepository carts;

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public OrderDTO createOrder(OrderCreateRequest request) {
        Long customerId = ownership.userId();
        if (request.getShippingAddress() == null || request.getShippingAddress().isBlank() || request.getShippingAddress().length() > 2000)
            throw new BadRequestException("Thông tin giao hàng là bắt buộc, tối đa 2000 ký tự");
        if (request.getPaymentMethod() != null && !"COD".equals(request.getPaymentMethod()))
            throw new BadRequestException("Chỉ hỗ trợ thanh toán COD");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BadRequestException("Đơn hàng phải có sản phẩm");
        for (OrderItemDTO item : request.getItems()) if (item == null || item.getPetId() != null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) throw new BadRequestException("Sản phẩm và số lượng phải hợp lệ; phiên bản này chưa hỗ trợ mua thú cưng");
        if (!ownership.hasRole("CUSTOMER") || java.util.stream.Stream.of("ADMIN", "SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN").anyMatch(ownership::hasRole))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ khách hàng được đặt mua");
        if (request.getPaymentMethod() != null && !"COD".equals(request.getPaymentMethod()))
            throw new BadRequestException("Chỉ hỗ trợ thanh toán COD");
        if (request.getItems() == null || request.getItems().isEmpty()) throw new BadRequestException("Đơn hàng phải có sản phẩm");
        java.util.Set<Long> petIds = new java.util.TreeSet<>();
        for (int index = 0; index < request.getItems().size(); index++) {
            OrderItemDTO item = request.getItems().get(index);
            if (item == null || !item.isItemReferenceValid() || !item.isPetQuantityValid() || item.getQuantity() == null || item.getQuantity() <= 0)
                throw new BadRequestException("items[" + index + "]: Chọn đúng một sản phẩm hoặc thú cưng và số lượng hợp lệ");
            if (item.getPetId() != null && !petIds.add(item.getPetId())) throw new BadRequestException("items[" + index + "].petId: Thú cưng bị trùng trong đơn");
        }
        Order order = Order.builder().customer(users.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")))
                .orderNumber("PC-" + java.util.UUID.randomUUID()).status(OrderStatus.PENDING).paymentStatus(PaymentStatus.PENDING)
                .paymentMethod("COD").shippingAddress(request.getShippingAddress()).notes(request.getNotes()).build();
        BigDecimal total = BigDecimal.ZERO;
        // Lock pets first, then product stock, in ascending IDs across create/cancel/deliver.
        java.util.List<Pet> reservedPets = new java.util.ArrayList<>();
        for (Long petId : petIds) {
            Pet pet = lockPet(petId);
            if (pet.getOwner() != null || pet.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE || pet.getReservedOrder() != null)
                throw new IllegalStateException("Thú cưng này không còn khả dụng");
            if (pet.getPrice() == null || pet.getPrice().signum() <= 0) throw new IllegalStateException("Thú cưng chưa được niêm yết giá");
            order.addItem(OrderItem.builder().itemType(OrderItemType.PET).pet(pet).quantity(1).unitPrice(pet.getPrice()).subtotal(pet.getPrice()).build());
            total = total.add(pet.getPrice()); reservedPets.add(pet);
        }
        // NEEDS DECISION: existing checkout has no promotion/loyalty or shipping fees; pet items get no new discounts.
        // Stable ordering reduces deadlocks; reserve stock in this same transaction.
        for (OrderItemDTO item : request.getItems().stream().filter(item -> item.getProductId() != null).sorted(java.util.Comparator.comparing(OrderItemDTO::getProductId)).toList()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) throw new BadRequestException("Sản phẩm và số lượng phải hợp lệ");
            Product product = products.findById(item.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
            if (!Boolean.TRUE.equals(product.getActive())) throw new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã ngừng bán");
            inventory.reduceStock(product.getId(), item.getQuantity());
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            order.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(item.getQuantity()).unitPrice(product.getPrice()).subtotal(subtotal).build());
            total = total.add(subtotal);
        }
        order.setSubtotal(total); order.setTotalAmount(total);
        users.findLockedById(customerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        carts.findByCustomerId(customerId).ifPresent(cart -> {
            java.util.Map<Long, Long> purchased = new java.util.HashMap<>();
            request.getItems().forEach(item -> purchased.merge(item.getProductId(), item.getQuantity().longValue(), Math::addExact));
            for (CartItem item : new java.util.ArrayList<>(cart.getItems())) {
                if (item.getProduct() == null) continue;
                long quantity = purchased.getOrDefault(item.getProduct().getId(), 0L);
                if (quantity == 0) continue;
                if (quantity >= item.getQuantity()) cart.removeItem(item);
                else item.setQuantity(item.getQuantity() - (int) quantity);
            }
            carts.saveAndFlush(cart);
        });
        return dto(orders.save(order));
        Order saved = orders.saveAndFlush(order);
        for (Pet pet : reservedPets) {
            pet.setAvailabilityStatus(AvailabilityStatus.PENDING); pet.setReservedOrder(saved); pets.save(pet);
        }
        // NEEDS DECISION: no expiry mechanism exists. Only explicit cancellation releases a reservation.
        return dto(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderDTO> getMyOrders(org.springframework.data.domain.Pageable pageable) {
        return orders.findByCustomerId(ownership.userId(), pageable).map(this::dto);
    }
    private Pet lockPet(Long id) {
        return pets.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thú cưng"));
    }
    private java.util.List<Pet> reservedPets(Order order) {
        var lockedPets = order.getItems().stream().filter(item -> item.getPet() != null)
                .map(item -> item.getPet().getId()).sorted().map(this::lockPet).toList();
        for (Pet pet : lockedPets) {
            if (pet.getOwner() != null || pet.getAvailabilityStatus() != AvailabilityStatus.PENDING || pet.getReservedOrder() == null
                    || !order.getId().equals(pet.getReservedOrder().getId()))
                throw new IllegalStateException("Trạng thái giữ chỗ thú cưng không khớp đơn hàng");
        }
        return lockedPets;
    }
    private Order locked(Long id) {
        return orders.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }
    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Long currentId = ownership.userId();
        Order order = (ownership.hasRole("ADMIN") || ownership.hasRole("SALES_STAFF")
                ? orders.findById(id) : orders.findByIdAndCustomerId(id, currentId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return dto(order);
    }
    private void requireStaff() {
        if (!ownership.hasRole("ADMIN") && !ownership.hasRole("SALES_STAFF")) throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xử lý đơn hàng");
    }
    @Override public OrderDTO confirmOrder(Long id) { requireStaff(); Order o=locked(id); state(o).confirm(o); return dto(orders.save(o)); }
    @Override public OrderDTO processOrder(Long id) { requireStaff(); Order o=locked(id); state(o).process(o); return dto(orders.save(o)); }
    @Override public OrderDTO shipOrder(Long id) { requireStaff(); Order o=locked(id); state(o).ship(o); return dto(orders.save(o)); }
    @Override public OrderDTO deliverOrder(Long id) {
        requireStaff(); Order o=locked(id);
        var reserved = reservedPets(o); state(o).deliver(o);
        for (Pet pet : reserved) {
            pet.setOwner(o.getCustomer()); pet.setAvailabilityStatus(AvailabilityStatus.SOLD); pet.setReservedOrder(null); pets.save(pet);
        }
        // NEEDS DECISION: COD is collected on delivery; no online payment claim is made.
        if ("COD".equals(o.getPaymentMethod())) {
            o.setPaymentStatus(PaymentStatus.COMPLETED);
            o.setPaidAt(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")));
        }
        return dto(orders.save(o));
    }
    @Override public OrderDTO cancelOrder(Long id) {
        Long currentId = ownership.userId();
        Order o = (ownership.hasRole("ADMIN") ? orders.findLockedById(id) : orders.findOwnedLockedById(id, currentId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        state(o).cancel(o);
        for (OrderItem item : o.getItems()) if (item.getProduct()!=null) {
        Order o=locked(id); ownership.requireUser(o.getCustomer().getId());
        // Keep legacy product-only cancellation behavior; pet retries never release another order's reservation.
        if (o.getStatus() == OrderStatus.CANCELLED && o.getItems().stream().anyMatch(item -> item.getPet() != null)) return dto(o);
        if (o.getStatus() == OrderStatus.DELIVERED) throw new InvalidStateTransitionException("Không thể hủy đơn đã giao");
        var reserved = reservedPets(o); state(o).cancel(o);
        for (Pet pet : reserved) {
            pet.setAvailabilityStatus(AvailabilityStatus.AVAILABLE); pet.setReservedOrder(null); pets.save(pet);
        }
        for (OrderItem item : o.getItems().stream().filter(line -> line.getProduct() != null).sorted(java.util.Comparator.comparing(line -> line.getProduct().getId())).toList()) {
            StockUpdateRequest stock = new StockUpdateRequest(); stock.setProductId(item.getProduct().getId()); stock.setQuantity(item.getQuantity()); stock.setReason("Hủy đơn " + o.getOrderNumber()); inventory.addStock(stock);
        }
        return dto(orders.save(o));
    }
    private OrderState state(Order o) {
        return switch(o.getStatus()) {
            case PENDING -> new PendingState(); case CONFIRMED -> new ConfirmedState(); case PROCESSING -> new ProcessingState();
            case SHIPPING -> new ShippingState(); case DELIVERED -> new DeliveredState(); case CANCELLED -> new CancelledState();
        };
    }
    private OrderDTO dto(Order o) {
        OrderDTO dto = new OrderDTO(); dto.setId(o.getId()); dto.setUserId(o.getCustomer().getId()); dto.setStatus(o.getStatus().name()); dto.setTotalPrice(o.getTotalAmount().doubleValue());
        dto.setNotes(o.getNotes()); dto.setOrderNumber(o.getOrderNumber()); dto.setShippingAddress(o.getShippingAddress()); dto.setPaymentMethod(o.getPaymentMethod());
        dto.setPaymentStatus(o.getPaymentStatus().name()); dto.setCreatedAt(o.getCreatedAt());
        dto.setItems(o.getItems().stream().map(item -> {
            OrderItemDTO d = new OrderItemDTO();
            if (item.getProduct() != null) { d.setProductId(item.getProduct().getId()); d.setProductName(item.getProduct().getName()); d.setImageUrl(item.getProduct().getImageUrl()); }
            if (item.getPet() != null) { d.setPetId(item.getPet().getId()); d.setPetName(item.getPet().getName()); d.setImageUrl(item.getPet().getImageUrl()); }
            d.setQuantity(item.getQuantity()); d.setPrice(item.getUnitPrice().doubleValue()); return d;
        }).toList()); return dto;
    }
}
