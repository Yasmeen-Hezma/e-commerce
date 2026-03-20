package com.ecommerce.e_commerce.commerce.pricing.decorator;

import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PromoCodeDecorator extends PriceDecorator {
    private final BigDecimal promoDiscountRate;

    public PromoCodeDecorator(PriceCalculator delegate, BigDecimal promoDiscountRate) {
        super(delegate);
        this.promoDiscountRate = promoDiscountRate;
    }

    @Override
    public PriceResult calculate() {
        PriceResult base = delegate.calculate();
        BigDecimal currentTotal = base.total();
        BigDecimal promoDiscount = currentTotal
                .multiply(promoDiscountRate)
                .setScale(2, RoundingMode.HALF_UP);
        return base.addPromoDiscount(promoDiscount);
    }
}
