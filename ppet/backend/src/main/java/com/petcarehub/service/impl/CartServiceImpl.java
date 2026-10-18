package com.petcarehub.service.impl;

import com.petcarehub.dto.cart.AddToCartRequest;
import com.petcarehub.dto.cart.CartDTO;
import com.petcarehub.dto.cart.CartItemDTO;
import com.petcarehub.dto.cart.UpdateCartItemRequest;
import com.petcarehub.service.CartService;
import org.springframework.stereotype.Service;
import com.petcarehub.exception.ResourceNotFoundException;
import com.petcarehub.exception.BadRequestException;
import com.petcarehub.entity.*;
import com.petcarehub.enums.OrderItemType;
import com.petcarehub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    private CartDTO toDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getCustomer().getId());
        dto.setItems(cart.getItems().stream().map(item -> {
            CartItemDTO itemDTO = new CartItemDTO();
            itemDTO.setId(item.getId());
            itemDTO.setQuantity(item.getQuantity());
            if (item.getProduct() != null) {
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductName(item.getProduct().getName());
                itemDTO.setPrice(item.getProduct().getPrice());
            }
            return itemDTO;
        }).toList());
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCurrentUserCart(Long userId) {
        return cartRepository.findByCustomerId(userId).map(this::toDTO).orElseGet(() -> {
            CartDTO dto = new CartDTO();
            dto.setUserId(userId);
            dto.setItems(List.of());
            return dto;
        });
    }

    @Override
    @Transactional
    public CartDTO addItemToCart(Long userId, AddToCartRequest request) {
        if (request.getProductId() == null || request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Sản phẩm và số lượng phải hợp lệ");
        }
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new BadRequestException("Sản phẩm hiện không được bán");
        }
        // Cart stock is a snapshot, not a reservation; checkout rechecks under its inventory lock.
        // Read it before locking the user to avoid reversing inventory -> audit-user lock order.
        int stock = inventoryRepository.findByProductId(product.getId()).map(Inventory::getQuantity).orElse(0);
        // Lock before reading the cart/items, including concurrent creation of the first cart.
        User customer = userRepository.findLockedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        Cart cart = cartRepository.findByCustomerId(userId)
                .orElseGet(() -> Cart.builder().customer(customer).build());
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);
        long quantity = (long) request.getQuantity() + (existing == null ? 0 : existing.getQuantity());
        if (quantity > stock) {
            throw new BadRequestException("Số lượng trong giỏ hàng vượt quá tồn kho (" + stock + ")");
        }
        if (existing != null) {
            existing.setQuantity((int) quantity);
        } else {
            cart.addItem(CartItem.builder().product(product).itemType(OrderItemType.PRODUCT)
                    .quantity((int) quantity).build());
        }
        return toDTO(cartRepository.saveAndFlush(cart));
    }

    @Override
    public CartDTO updateCartItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        // TODO: Implement updating quantity
        return new CartDTO();
    }

    @Override
    public void removeItemFromCart(Long userId, Long cartItemId) {
        // TODO: Implement removing item
    }

    @Override
    public void clearCart(Long userId) {
        // TODO: Implement clearing cart
    }
}
