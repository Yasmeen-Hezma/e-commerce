package com.ecommerce.e_commerce.commerce.pricing.calculator;

import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RequiredArgsConstructor
public class BasePriceCalculator implements PriceCalculator {
    private final List<CartItem> items;

    @Override
    public PriceResult calculate() {
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal totalProductDiscount = BigDecimal.ZERO;
        for (CartItem item : items) {
            BigDecimal itemPrice = item.getProduct().getPrice();
            int quantity = item.getQuantity();
            BigDecimal itemSubTotal = itemPrice.multiply(BigDecimal.valueOf(quantity));
            // Calculate product-level discount
            BigDecimal productDiscountPercent = item.getProduct().getDiscount();
            if (productDiscountPercent != null && productDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal itemDiscount = itemSubTotal.multiply(productDiscountPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
                totalProductDiscount = totalProductDiscount.add(itemDiscount);
            }
            subTotal = subTotal.add(itemSubTotal);
        }
        subTotal = subTotal.setScale(2, RoundingMode.HALF_UP);
        totalProductDiscount = totalProductDiscount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subTotal.subtract(totalProductDiscount);
        return new PriceResult(
                subTotal,
                totalProductDiscount,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                total);
    }
}
