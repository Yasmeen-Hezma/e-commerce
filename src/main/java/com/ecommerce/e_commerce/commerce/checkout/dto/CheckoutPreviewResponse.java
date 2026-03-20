package com.ecommerce.e_commerce.commerce.checkout.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Builder
public class CheckoutPreviewResponse {
    private BigDecimal subTotal;
    private BigDecimal productDiscounts;
    private BigDecimal firstOrderDiscount;
    private BigDecimal promoDiscount;
    private BigDecimal shipping;
    private BigDecimal tax;
    private BigDecimal total;
    private Boolean isFirstOrder;
    private Boolean freeShippingApplied;
}
