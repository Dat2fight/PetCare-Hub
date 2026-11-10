package com.petcarehub.controller;

import com.petcarehub.entity.Product;
import com.petcarehub.repository.ProductRepository;
import com.petcarehub.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeHighlightsController {
    private final ProductRepository products;
    private final ReviewRepository reviews;

    public record ProductCard(Long id, String name, String description, String imageUrl, BigDecimal price) {
        static ProductCard from(Product product) {
            return new ProductCard(product.getId(), product.getName(), product.getDescription(), product.getImageUrl(), product.getPrice());
        }
    }
    public record FeaturedProducts(boolean bestSelling, List<ProductCard> items) {}
    public record CustomerReview(Long id, String displayName, int rating, String comment) {}

    @GetMapping("/products")
    public FeaturedProducts products() {
        var ranked = products.topDeliveredProductIds(PageRequest.of(0, 6));
        if (ranked.isEmpty()) return new FeaturedProducts(false,
                products.findTop6ByActiveTrueOrderByIdDesc().stream().map(ProductCard::from).toList());
        Map<Long,Product> byId = products.findAllById(ranked).stream().filter(p -> Boolean.TRUE.equals(p.getActive()))
                .collect(Collectors.toMap(Product::getId, p -> p));
        return new FeaturedProducts(true, ranked.stream().map(byId::get).filter(java.util.Objects::nonNull).map(ProductCard::from).toList());
    }

    @GetMapping("/reviews")
    public List<CustomerReview> reviews() {
        return reviews.findByApprovedTrue(PageRequest.of(0, 3, Sort.by("id").descending())).stream()
                .map(r -> new CustomerReview(r.getId(), initials(r.getCustomer().getFirstName()), r.getRating(), r.getComment())).toList();
    }

    private static String initials(String name) {
        return name == null || name.isBlank() ? "Khách hàng" : name.strip().substring(0, name.strip().offsetByCodePoints(0, 1)) + ".";
    }
}
