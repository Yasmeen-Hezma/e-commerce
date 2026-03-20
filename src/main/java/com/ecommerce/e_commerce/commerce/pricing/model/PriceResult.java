package com.ecommerce.e_commerce.commerce.pricing.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PriceResult(
        BigDecimal subtotal,
        BigDecimal productDiscount,
        BigDecimal firstOrderDiscount,
        BigDecimal promoDiscount,
        BigDecimal shipping,
        BigDecimal tax,
        BigDecimal total
) {

    public PriceResult addFirstOrderDiscount(BigDecimal value) {
        return new PriceResult(subtotal, productDiscount, firstOrderDiscount.add(value), promoDiscount, shipping, tax, total.subtract(value));
    }

    public PriceResult addPromoDiscount(BigDecimal value) {
        return new PriceResult(subtotal, productDiscount, firstOrderDiscount, promoDiscount.add(value), shipping, tax, total.subtract(value));
    }

    public PriceResult addShipping(BigDecimal value) {
        return new PriceResult(subtotal, productDiscount, firstOrderDiscount, promoDiscount, shipping.add(value), tax, total.add(value));
    }

    public PriceResult addTax(BigDecimal value) {
        return new PriceResult(subtotal, productDiscount, firstOrderDiscount, promoDiscount, shipping, tax.add(value), total.add(value));
    }
}
