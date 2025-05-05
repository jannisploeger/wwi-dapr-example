package dev.ploeger.dapr.shop.model;

import java.util.List;

public record Order(
        String name,
        String address,
        String email,
        String phone,
        String paymentMethod,
        List<Sweet> sweets
) {
}
