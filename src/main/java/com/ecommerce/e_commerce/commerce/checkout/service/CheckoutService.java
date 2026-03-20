package com.ecommerce.e_commerce.commerce.checkout.service;

import com.ecommerce.e_commerce.commerce.checkout.dto.ApplyPromoRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface CheckoutService {
    CheckoutPreviewResponse previewCheckout(HttpServletRequest request, CheckoutPreviewRequest checkoutPreviewRequest);

    CheckoutPreviewResponse applyPromoCode(HttpServletRequest request, ApplyPromoRequest applyPromoRequest);
}
