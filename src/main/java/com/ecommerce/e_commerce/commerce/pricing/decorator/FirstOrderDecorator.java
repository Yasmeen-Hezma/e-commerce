package com.ecommerce.e_commerce.commerce.pricing.decorator;

import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;

import java.math.BigDecimal;

public class FirstOrderDecorator extends PriceDecorator {
    private final BigDecimal discountAmount;

    public FirstOrderDecorator(PriceCalculator delegate, BigDecimal discountAmount) {
        super(delegate);
        this.discountAmount = discountAmount;
    }

    @Override
    public PriceResult calculate() {
        PriceResult base = delegate.calculate();
        BigDecimal discountToApply = discountAmount.min(base.total());
        return base.addFirstOrderDiscount(discountToApply);
    }
}
