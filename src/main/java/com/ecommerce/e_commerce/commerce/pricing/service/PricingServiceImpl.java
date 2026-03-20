package com.ecommerce.e_commerce.commerce.pricing.service;

import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.pricing.calculator.BasePriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.calculator.PriceCalculator;
import com.ecommerce.e_commerce.commerce.pricing.decorator.FirstOrderDecorator;
import com.ecommerce.e_commerce.commerce.pricing.decorator.PromoCodeDecorator;
import com.ecommerce.e_commerce.commerce.pricing.decorator.ShippingDecorator;
import com.ecommerce.e_commerce.commerce.pricing.decorator.TaxDecorator;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.common.exception.InvalidOperationException;
import com.ecommerce.e_commerce.user.profile.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.ecommerce.e_commerce.common.utils.Constants.*;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {
    @Override
    public PriceResult calculateCart(List<CartItem> items) {
        return new BasePriceCalculator(items).calculate();
    }

    @Override
    public BigDecimal calculatePriceSnapshot(Product product) {
        BigDecimal price = product.getPrice();
        BigDecimal discount = product.getDiscount();
        if (discount == null || discount.compareTo(BigDecimal.ZERO) < 0) {
            return price;
        }
        BigDecimal discountAmount = price
                .multiply(discount.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        return price.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PriceResult calculateCheckoutPreview(List<CartItem> items, User user, String governorate) {
        PriceCalculator calculator = baseCalculator(items, user);
        calculator = applyShippingAndTax(governorate, calculator);
        return calculator.calculate();
    }

    @Override
    public PriceResult calculateWithPromo(List<CartItem> items, User user, String promoCode, String governorate) {
        PriceCalculator calculator = baseCalculator(items, user);
        calculator = applyPromoCode(promoCode, calculator);
        calculator = applyShippingAndTax(governorate, calculator);
        return calculator.calculate();
    }

    private PriceCalculator baseCalculator(List<CartItem> items, User user) {
        PriceCalculator calculator = new BasePriceCalculator(items);
        if (isFirstOrder(user)) {
            calculator = new FirstOrderDecorator(calculator, FIRST_ORDER_DISCOUNT);
        }
        return calculator;
    }

    private PriceCalculator applyShippingAndTax(String governorate, PriceCalculator calculator) {
        PriceResult currentPrice = calculator.calculate();
        BigDecimal shippingFee = calculateShippingFee(currentPrice.total(), governorate);
        calculator = new TaxDecorator(calculator);
        calculator = new ShippingDecorator(calculator, shippingFee);
        return calculator;
    }

    private PriceCalculator applyPromoCode(String promoCode, PriceCalculator calculator) {
        BigDecimal promoRate = validateAndGetPromoRate(promoCode);
        return new PromoCodeDecorator(calculator, promoRate);
    }


    private boolean isFirstOrder(User user) {
        return user.getOrders().stream().noneMatch(order -> order.getStatus() == OrderStatus.CONFIRMED);
    }

    private BigDecimal calculateShippingFee(BigDecimal currentTotal, String governorate) {
        if (currentTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }
        return switch (governorate.toLowerCase()) {
            case "cairo", "giza" -> CAIRO_SHIPPING;
            case "alexandria" -> ALEXANDRIA_SHIPPING;
            default -> DEFAULT_SHIPPING;
        };
    }

    private BigDecimal validateAndGetPromoRate(String promoCode) {
        return switch (promoCode.toUpperCase()) {
            case "SAVE15" -> SAVE_15_PROMO_CODE;
            case "SAVE20" -> SAVE_20_PROMO_CODE;
            default -> throw new InvalidOperationException(INVALID_PROMO_CODE);
        };
    }
}
