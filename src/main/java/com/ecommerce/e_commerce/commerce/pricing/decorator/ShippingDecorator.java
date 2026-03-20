package com.ecommerce.e_commerce.commerce.pricing.decorator;

import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;

import java.math.BigDecimal;

public class ShippingDecorator extends PriceDecorator {
    private final BigDecimal shippingFee;

    public ShippingDecorator(PriceCalculator delegate, BigDecimal shippingFee) {
        super(delegate);
        this.shippingFee = shippingFee;
    }

    @Override
    public PriceResult calculate() {
        PriceResult base = delegate.calculate();
        return base.addShipping(shippingFee);
    }
}
