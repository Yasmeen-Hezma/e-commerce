package com.ecommerce.e_commerce.commerce.checkout.service;

import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.service.CartService;
import com.ecommerce.e_commerce.commerce.checkout.dto.ApplyPromoRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewResponse;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;


@Service
@RequiredArgsConstructor
public class CheckoutServiceImpl implements CheckoutService {
    private final CartService cartService;
    private final PricingService pricingService;
    private final UserService userService;

    @Override
    public CheckoutPreviewResponse previewCheckout(HttpServletRequest request, CheckoutPreviewRequest checkoutPreviewRequest) {
        User user = userService.getUserByRequest(request);
        Cart cart = cartService.getCartByUser(request);
        PriceResult pricing = pricingService
                .calculateCheckoutPreview(cart.getCartItems(), user, checkoutPreviewRequest.getGovernorate());
        return buildCheckoutPreviewResponse(pricing);
    }

    @Override
    public CheckoutPreviewResponse applyPromoCode(HttpServletRequest request, ApplyPromoRequest applyPromoRequest) {
        User user = userService.getUserByRequest(request);
        Cart cart = cartService.getCartByUser(request);
        PriceResult pricing = pricingService
                .calculateWithPromo(cart.getCartItems(), user, applyPromoRequest.getPromoCode(), applyPromoRequest.getGovernorate());
        return buildCheckoutPreviewResponse(pricing);
    }

    private CheckoutPreviewResponse buildCheckoutPreviewResponse(PriceResult pricing) {
        boolean isFirstOrder = pricing.firstOrderDiscount().compareTo(BigDecimal.ZERO) > 0;
        boolean freeShipping = pricing.shipping().compareTo(BigDecimal.ZERO) == 0;
        return CheckoutPreviewResponse
                .builder()
                .subTotal(pricing.subtotal())
                .productDiscounts(pricing.productDiscount())
                .firstOrderDiscount(pricing.firstOrderDiscount())
                .promoDiscount(pricing.promoDiscount())
                .shipping(pricing.shipping())
                .tax(pricing.tax())
                .total(pricing.total())
                .isFirstOrder(isFirstOrder)
                .freeShippingApplied(freeShipping)
                .build();
    }
}
