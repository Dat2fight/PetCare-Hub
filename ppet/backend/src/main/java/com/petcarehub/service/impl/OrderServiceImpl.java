package com.petcarehub.service.impl;

import com.petcarehub.dto.order.*;
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
    private final PackageCheckout packages;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final PetRepository pets;
    private final UserRepository users;
    private final InventoryService inventory;
    private final Ownership ownership;
    private final CartRepository carts;
    private final CheckoutPricing pricing;
    private final java.time.Clock clock;
    private final com.petcarehub.config.OrderExpiryConfig.Settings expiry;
    private final NotificationRepository notifications;

    @Override
    @Transactional(readOnly = true)
    public OrderQuote quote(QuoteRequest request) {
        Long userId = ownership.userId();
        if (PackageCheckout.requested(request)) return packages.quote(packages.prepare(request,false));
        var lines = new java.util.ArrayList<OrderQuote.Line>();
        BigDecimal merchandise = BigDecimal.ZERO;
        BigDecimal animals = BigDecimal.ZERO;
        for (OrderItemDTO line : request.getItems()) {
            if (line.getPetId() != null) {
                Pet pet = pets.findById(line.getPetId()).filter(p -> p.getOwner() == null && p.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE)
                        .orElseThrow(() -> new ResourceNotFoundException("Thú cưng không còn khả dụng"));
                if (pet.getPrice() == null || pet.getPrice().signum() <= 0 || pet.getReservedOrder() != null)
                    throw new IllegalStateException("Thú cưng chưa sẵn sàng để đặt");
                lines.add(new OrderQuote.Line("PET", pet.getId(), 1, pet.getPrice()));
                animals = animals.add(pet.getPrice());
            } else {
                Product product = products.findById(line.getProductId()).filter(p -> Boolean.TRUE.equals(p.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm đã ngừng bán"));
                if (!inventory.checkStock(product.getId(), line.getQuantity()))
                    throw new IllegalStateException("Không đủ tồn kho cho " + product.getName());
                lines.add(new OrderQuote.Line("PRODUCT", product.getId(), line.getQuantity(), product.getPrice()));
                merchandise = merchandise.add(product.getPrice().multiply(BigDecimal.valueOf(line.getQuantity())));
            }
        }
        return pricing.calculate(userId, merchandise.add(animals), merchandise, request.getPromotionCode(), request.getPointsToUse(), false).withLines(lines);
    }

    @Override
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public OrderDTO createOrder(OrderCreateRequest request) {
        Long customerId = ownership.userId();
        if (!ownership.hasRole("CUSTOMER") || java.util.stream.Stream.of("ADMIN", "SALES_STAFF", "WAREHOUSE_STAFF", "VETERINARIAN").anyMatch(ownership::hasRole))
            throw new org.springframework.security.access.AccessDeniedException("Chỉ khách hàng được đặt mua");
        if (!PackageCheckout.requested(request) && request.getShipping() == null)
            throw new BadRequestException("Vui lòng điền thông tin giao hàng");
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
        users.findLockedById(customerId).orElseThrow();
        if (request.getCheckoutKey() == null) throw new BadRequestException("Thiếu Idempotency-Key");
        String key = request.getCheckoutKey().toString();
        String fingerprint = checkoutFingerprint(request);
        var prior = orders.findByCustomerIdAndCheckoutKey(customerId, key);
        if (prior.isPresent()) {
            if (!fingerprint.equals(prior.get().getCheckoutFingerprint()))
                throw new CheckoutKeyConflictException();
            var replay = dto(prior.get());
            replay.setReplay(true);
            return replay;
        }
        Order order = Order.builder().checkoutKey(key).checkoutFingerprint(fingerprint).customer(users.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản")))
                .orderNumber("PC-" + java.util.UUID.randomUUID()).status(OrderStatus.PENDING).paymentStatus(PaymentStatus.PENDING)
                .paymentMethod("COD").shipping(request.getShipping()).notes(request.getNotes()).build();
        if (PackageCheckout.requested(request)) {
            var line=packages.prepare(request,true);
            order.addItem(line);
            order.setSubtotal(line.getSubtotal());
            order.setTotalAmount(line.getSubtotal());
            order.setShippingFee(BigDecimal.ZERO);
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setPointsUsed(0);
            order.setPointsDiscount(BigDecimal.ZERO);
            return dto(orders.saveAndFlush(order));
        }
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
        BigDecimal petTotal = total;
        // Stable ordering reduces deadlocks; reserve stock in this same transaction.
        for (OrderItemDTO item : request.getItems().stream().filter(item -> item.getProductId() != null).sorted(java.util.Comparator.comparing(OrderItemDTO::getProductId)).toList()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) throw new BadRequestException("Sản phẩm và số lượng phải hợp lệ");
            Product product = products.findById(item.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
            if (!Boolean.TRUE.equals(product.getActive())) throw new ResourceNotFoundException("Sản phẩm không tồn tại hoặc đã ngừng bán");
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            order.addItem(OrderItem.builder().itemType(OrderItemType.PRODUCT).product(product).quantity(item.getQuantity()).unitPrice(product.getPrice()).subtotal(subtotal).build());
            total = total.add(subtotal);
        }
        var summary = pricing.calculate(customerId, total, total.subtract(petTotal), request.getPromotionCode(), request.getPointsToUse(), true);
        if (request.getQuoteVersion() != null) {
            var lines = order.getItems().stream().map(i -> new OrderQuote.Line(i.getItemType().name(),
                    i.getPet() == null ? i.getProduct().getId() : i.getPet().getId(), i.getQuantity(), i.getUnitPrice())).toList();
            if (!summary.withLines(lines).getQuoteVersion().equals(request.getQuoteVersion()))
                throw new IllegalStateException("Báo giá đã thay đổi. Tổng mới " + summary.total().toPlainString() + " ₫. Vui lòng xem lại đơn.");
        }
        order.setSubtotal(total);
        order.setDiscountAmount(summary.discount());
        order.setPointsUsed(summary.pointsApplied());
        order.setPointsDiscount(summary.pointsDiscount());
        order.setShippingFee(summary.shippingFee());
        order.setTotalAmount(summary.total());
        if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank())
            order.setPromotionCode(request.getPromotionCode().trim());
        carts.findByCustomerId(customerId).ifPresent(cart -> {
            java.util.Map<Long, Long> purchased = new java.util.HashMap<>();
            request.getItems().stream().filter(line -> line.getProductId() != null).forEach(item -> purchased.merge(item.getProductId(), item.getQuantity().longValue(), Math::addExact));
            for (CartItem item : new java.util.ArrayList<>(cart.getItems())) {
                if (item.getPet() != null && petIds.contains(item.getPet().getId())) cart.removeItem(item);
                if (item.getProduct() == null) continue;
                long quantity = purchased.getOrDefault(item.getProduct().getId(), 0L);
                if (quantity == 0) continue;
                if (quantity >= item.getQuantity()) cart.removeItem(item);
                else item.setQuantity(item.getQuantity() - (int) quantity);
            }
            carts.saveAndFlush(cart);
        });
        if (!reservedPets.isEmpty()) order.setExpiresAt(java.time.LocalDateTime.now(clock).plusHours(expiry.holdHours()));
        Order saved = orders.saveAndFlush(order);
        for (OrderItem item : saved.getItems()) {
            if (item.getProduct() != null) inventory.reserveForOrder(item.getProduct().getId(), item.getQuantity(), saved.getId());
        }
        pricing.reserve(saved);
        for (Pet pet : reservedPets) {
            pet.setAvailabilityStatus(AvailabilityStatus.PENDING); pet.setReservedOrder(saved); pets.save(pet);
        }
        return dto(saved);
    }
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<OrderDTO> getMyOrders(org.springframework.data.domain.Pageable pageable) {
        return orders.findByCustomerId(ownership.userId(), pageable).map(this::dto);
    }
    private String checkoutFingerprint(OrderCreateRequest request) {
        var lines = request.getItems().stream()
                .map(i -> i.getCarePackageId()==null ? java.util.Arrays.asList(i.getProductId(), i.getPetId(), i.getQuantity())
                        : java.util.Arrays.asList("CARE_PACKAGE",i.getCarePackageId(),i.getBeneficiaryPetId(),i.getQuantity()))
                .sorted(java.util.Comparator.comparing(Object::toString)).toList();
        var fields = java.util.Arrays.asList(request.getShipping(), request.getNotes(),
                request.getPromotionCode(), request.getPointsToUse(), request.getQuoteVersion(), lines);
        try {
            byte[] payload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(fields);
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (java.security.NoSuchAlgorithmException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Không thể xác minh yêu cầu đặt hàng", e);
        }
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
    @Override public OrderDTO confirmOrder(Long id) { requireStaff(); Order o=locked(id);
        if (isDue(o, java.time.LocalDateTime.now(clock))) throw new IllegalStateException("Đơn hàng đã quá hạn xác nhận");
        state(o).confirm(o); o.setConfirmedAt(java.time.LocalDateTime.now(clock)); return dto(orders.save(o)); }
    @Override public OrderDTO processOrder(Long id) { requireStaff(); Order o=locked(id); state(o).process(o); o.setProcessingAt(java.time.LocalDateTime.now(clock)); return dto(orders.save(o)); }
    @Override public OrderDTO shipOrder(Long id) { requireStaff(); Order o=locked(id); state(o).ship(o); o.setShippedAt(java.time.LocalDateTime.now(clock)); return dto(orders.save(o)); }
    @Override public OrderDTO deliverOrder(Long id) {
        requireStaff(); Order o=locked(id);
        var packageLine=o.getItems().stream().filter(i -> i.getCarePackage()!=null).findFirst();
        if (packageLine.isPresent() && o.getStatus()==OrderStatus.DELIVERED && o.getPaymentStatus()==PaymentStatus.COMPLETED) return dto(o);
        var reserved = reservedPets(o); state(o).deliver(o); o.setDeliveredAt(java.time.LocalDateTime.now(clock));
        for (Pet pet : reserved) {
            pet.setOwner(o.getCustomer()); pet.setAvailabilityStatus(AvailabilityStatus.SOLD); pet.setReservedOrder(null); pets.save(pet);
        }
        if ("COD".equals(o.getPaymentMethod())) {
            pricing.earn(o);
            o.setPaymentStatus(PaymentStatus.COMPLETED);
            o.setPaidAt(java.time.LocalDateTime.now(clock));
            packageLine.ifPresent(line -> packages.activate(o,line));
        }
        return dto(orders.save(o));
    }
    @Override public OrderDTO cancelOrder(Long id) {
        Long currentId = ownership.userId();
        Order o = (ownership.hasRole("ADMIN") ? orders.findLockedById(id) : orders.findOwnedLockedById(id, currentId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
        return cancelLocked(o);
    }

    private boolean isDue(Order order, java.time.LocalDateTime now) {
        return order.getStatus() == OrderStatus.PENDING && order.getPaymentStatus() == PaymentStatus.PENDING
                && order.getExpiresAt() != null && !order.getExpiresAt().isAfter(now);
    }

    @Override
    public boolean expireIfDue(Long id) {
        Order order = locked(id);
        var now = java.time.LocalDateTime.now(clock);
        if (!isDue(order, now)) return false;
        order.setExpiredAt(now);
        cancelLocked(order);
        notifications.save(Notification.builder().user(order.getCustomer()).type(NotificationType.ORDER_UPDATE)
                .title("Đơn hàng hết hạn").message("Đơn " + order.getOrderNumber() + " đã hết hạn chờ xác nhận.").build());
        return true;
    }

    private OrderDTO cancelLocked(Order o) {
        // Keep legacy product-only cancellation behavior; pet retries never release another order's reservation.
        if (o.getStatus() == OrderStatus.CANCELLED && o.getItems().stream().anyMatch(item -> item.getPet() != null)) return dto(o);
        if (o.getStatus() == OrderStatus.DELIVERED) throw new InvalidStateTransitionException("Không thể hủy đơn đã giao");
        pricing.release(o);
        var reserved = reservedPets(o); state(o).cancel(o); o.setCancelledAt(java.time.LocalDateTime.now(clock));
        for (Pet pet : reserved) {
            pet.setAvailabilityStatus(AvailabilityStatus.AVAILABLE); pet.setReservedOrder(null); pets.save(pet);
        }
        for (OrderItem item : o.getItems().stream().filter(line -> line.getProduct() != null).sorted(java.util.Comparator.comparing(line -> line.getProduct().getId())).toList()) {
            inventory.restoreForOrder(item.getProduct().getId(), item.getQuantity(), o.getId());
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
        dto.setSubtotal(o.getSubtotal());
        dto.setPromotionCode(o.getPromotionCode());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setPointsUsed(o.getPointsUsed());
        dto.setPointsDiscount(o.getPointsDiscount());
        dto.setShippingFee(o.getShippingFee());
        dto.setExpiresAt(o.getExpiresAt()); dto.setExpiredAt(o.getExpiredAt());
        dto.setNotes(o.getNotes()); dto.setOrderNumber(o.getOrderNumber()); dto.setCheckoutKey(o.getCheckoutKey()); dto.setShipping(o.getShipping());
        dto.setConfirmedAt(o.getConfirmedAt());
        dto.setProcessingAt(o.getProcessingAt());
        dto.setShippedAt(o.getShippedAt());
        dto.setDeliveredAt(o.getDeliveredAt());
        dto.setCancelledAt(o.getCancelledAt());
        dto.setShippingAddress(o.getShipping() == null ? o.getShippingAddress() : o.getShipping().formatted()); dto.setPaymentMethod(o.getPaymentMethod());
        dto.setPaymentStatus(o.getPaymentStatus().name()); dto.setCreatedAt(o.getCreatedAt());
        dto.setItems(o.getItems().stream().map(item -> {
            OrderItemDTO d = new OrderItemDTO();
            if (item.getProduct() != null) { d.setProductId(item.getProduct().getId()); d.setProductName(item.getProduct().getName()); d.setImageUrl(item.getProduct().getImageUrl()); }
            if (item.getPet() != null) { d.setPetId(item.getPet().getId()); d.setPetName(item.getPet().getName()); d.setImageUrl(item.getPet().getImageUrl()); }
            if (item.getCarePackage()!=null) {
                d.setCarePackageId(item.getCarePackage().getId());
                d.setBeneficiaryPetId(item.getBeneficiaryPet().getId());
                d.setPackageName(item.getPackageName());
            }
            d.setQuantity(item.getQuantity()); d.setPrice(item.getUnitPrice().doubleValue()); return d;
        }).toList()); return dto;
    }
}
