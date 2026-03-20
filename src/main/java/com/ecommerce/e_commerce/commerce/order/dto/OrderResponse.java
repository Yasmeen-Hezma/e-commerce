package com.ecommerce.e_commerce.commerce.order.dto;

import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.payment.dto.PaymentResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Setter
@Getter
@Builder
public class OrderResponse {
    // Order info
    private Long id;
    private Long userId;
    private List<OrderItemResponse> items;
    private OrderStatus status;
    private Instant createdAt;
    // Pricing

    private BigDecimal subtotal;
    private BigDecimal productDiscounts;
    private BigDecimal firstOrderDiscount;
    private BigDecimal tax;
    private BigDecimal shipping;
    private BigDecimal promoDiscount;
    private BigDecimal totalPrice;
    private String promoCodeUsed;
    // Relationships
    private PaymentResponse payment;
    private UserAddressDto address;
}
