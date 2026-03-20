package com.ecommerce.e_commerce.commerce.checkout.controller;

import com.ecommerce.e_commerce.commerce.checkout.dto.ApplyPromoRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewRequest;
import com.ecommerce.e_commerce.commerce.checkout.dto.CheckoutPreviewResponse;
import com.ecommerce.e_commerce.commerce.checkout.service.CheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/checkout")
@Tag(name = "Checkout", description = "Checkout Management APIs")
@RequiredArgsConstructor
public class CheckoutController {
    private final CheckoutService checkoutService;

    @PostMapping("/preview")
    @Operation(summary = "Preview checkout details")
    public ResponseEntity<CheckoutPreviewResponse> previewCheckout(HttpServletRequest request, @RequestBody CheckoutPreviewRequest checkoutPreviewRequest) {
        return ResponseEntity.ok(checkoutService.previewCheckout(request, checkoutPreviewRequest));
    }

    @PostMapping("/apply-promo")
    @Operation(summary = "Apply promo code to checkout")
    public ResponseEntity<CheckoutPreviewResponse> applyPromoCode(HttpServletRequest request, @RequestBody ApplyPromoRequest applyPromoRequest) {
        return ResponseEntity.ok(checkoutService.applyPromoCode(request, applyPromoRequest));
    }
}
