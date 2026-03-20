package com.ecommerce.e_commerce.commerce.pricing.decorator;

import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TaxDecorator extends PriceDecorator {
    private static final BigDecimal DEFAULT_TAX_RATE = BigDecimal.valueOf(0.14);
    private final BigDecimal taxRate;

    // Uses default 14% tax rate
    public TaxDecorator(PriceCalculator delegate) {
        super(delegate);
        this.taxRate = DEFAULT_TAX_RATE;
    }

    @Override
    public PriceResult calculate() {
        PriceResult base = delegate.calculate();
        BigDecimal currentTotal = base.total();
        BigDecimal taxAmount = currentTotal.multiply(taxRate)
                .setScale(2, RoundingMode.HALF_UP);
        return base.addTax(taxAmount);
    }
}
