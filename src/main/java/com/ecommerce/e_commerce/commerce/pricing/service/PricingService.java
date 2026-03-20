package com.ecommerce.e_commerce.commerce.pricing.service;

import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.user.profile.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface PricingService {
    PriceResult calculateCart(List<CartItem> items);

    BigDecimal calculatePriceSnapshot(Product product);

    PriceResult calculateCheckoutPreview(List<CartItem> items, User user, String governorate);

    PriceResult calculateWithPromo(List<CartItem> items, User user, String promoCode, String governorate);
}
