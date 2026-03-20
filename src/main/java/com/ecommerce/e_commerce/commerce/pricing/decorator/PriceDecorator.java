package com.ecommerce.e_commerce.commerce.pricing.decorator;

import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class PriceDecorator implements PriceCalculator{
    protected final PriceCalculator delegate;
}
