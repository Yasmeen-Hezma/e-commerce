package com.ecommerce.e_commerce.commerce.cart.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class CartItemResponse {
    private Long productId;
    private String productName;
    private Integer maxQuantity;
    private String image;
    private Integer quantity;
    private BigDecimal originalPrice;
    private BigDecimal discountPercent;
    private BigDecimal priceSnapshot;
    private BigDecimal lineTotal;
}
