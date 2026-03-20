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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {
    @Mock
    private CartService cartService;
    @Mock
    private PricingService pricingService;
    @Mock
    private UserService userService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    private User user;
    private Cart cart;
    private PriceResult priceResult;

    @BeforeEach
    void setUp() {
        user = User.builder().userId(1L).build();
        cart = Cart.builder().cartItems(new ArrayList<>()).build();

        priceResult = PriceResult
                .builder()
                .subtotal(new BigDecimal("100.00"))
                .productDiscount(new BigDecimal("10.00"))
                .firstOrderDiscount(new BigDecimal("50.00"))
                .promoDiscount(BigDecimal.ZERO)
                .shipping(new BigDecimal("30.00"))
                .tax(new BigDecimal("5.60"))
                .total(new BigDecimal("75.60"))
                .build();
    }

    @Test
    void previewCheckout_ShouldCallPricingService_WhenValidRequest() {
        // Arrange
        CheckoutPreviewRequest checkoutRequest = new CheckoutPreviewRequest("Cairo");
        when(userService.getUserByRequest(any())).thenReturn(user);
        when(cartService.getCartByUser(any())).thenReturn(cart);
        when(pricingService.calculateCheckoutPreview(any(), any(), eq("Cairo"))).thenReturn(priceResult);
        // Act
        CheckoutPreviewResponse result = checkoutService.previewCheckout(request, checkoutRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubTotal()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getProductDiscounts()).isEqualTo(new BigDecimal("10.00"));
        assertThat(result.getFirstOrderDiscount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getPromoDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getShipping()).isEqualTo(new BigDecimal("30.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("5.60"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("75.60"));
        assertThat(result.getIsFirstOrder()).isTrue();
        assertThat(result.getFreeShippingApplied()).isFalse();
        verify(userService).getUserByRequest(any());
        verify(cartService).getCartByUser(any());
        verify(pricingService).calculateCheckoutPreview(cart.getCartItems(), user, "Cairo");

    }

    @Test
    void previewCheckout_ShouldCallPricingServiceWithPromo_WhenValidRequest() {
        // Arrange
        ApplyPromoRequest applyPromoRequest = new ApplyPromoRequest("Cairo", "SAVE20");
        priceResult = PriceResult
                .builder()
                .subtotal(new BigDecimal("100.00"))
                .productDiscount(new BigDecimal("10.00"))
                .firstOrderDiscount(new BigDecimal("50.00"))
                .promoDiscount(new BigDecimal("8.00"))
                .shipping(new BigDecimal("30.00"))
                .tax(new BigDecimal("5.04"))
                .total(new BigDecimal("67.04"))
                .build();
        when(userService.getUserByRequest(any())).thenReturn(user);
        when(cartService.getCartByUser(any())).thenReturn(cart);
        when(pricingService.calculateWithPromo(any(), any(), eq("SAVE20"), eq("Cairo"))).thenReturn(priceResult);
        // Act
        CheckoutPreviewResponse result = checkoutService.applyPromoCode(request, applyPromoRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubTotal()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.getProductDiscounts()).isEqualTo(new BigDecimal("10.00"));
        assertThat(result.getFirstOrderDiscount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.getPromoDiscount()).isEqualTo(new BigDecimal("8.00"));
        assertThat(result.getShipping()).isEqualTo(new BigDecimal("30.00"));
        assertThat(result.getTax()).isEqualTo(new BigDecimal("5.04"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("67.04"));
        assertThat(result.getIsFirstOrder()).isTrue();
        assertThat(result.getFreeShippingApplied()).isFalse();
        verify(userService).getUserByRequest(any());
        verify(cartService).getCartByUser(any());
        verify(pricingService).calculateWithPromo(cart.getCartItems(), user, "SAVE20", "Cairo");
    }

    @Test
    void previewCheckout_ShouldApplyFreeShipping_WhenFreeAboveThreshold() {
        // Arrange
        CheckoutPreviewRequest checkoutRequest = new CheckoutPreviewRequest("Cairo");
        priceResult = PriceResult
                .builder()
                .subtotal(new BigDecimal("600.00"))
                .productDiscount(BigDecimal.ZERO)
                .firstOrderDiscount(BigDecimal.ZERO)
                .promoDiscount(BigDecimal.ZERO)
                .shipping(BigDecimal.ZERO)
                .tax(new BigDecimal("84.00"))
                .total(new BigDecimal("684.00"))
                .build();
        when(userService.getUserByRequest(any())).thenReturn(user);
        when(cartService.getCartByUser(any())).thenReturn(cart);
        when(pricingService.calculateCheckoutPreview(any(), any(), eq("Cairo"))).thenReturn(priceResult);
        // Act
        CheckoutPreviewResponse result = checkoutService.previewCheckout(request, checkoutRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSubTotal()).isEqualTo(new BigDecimal("600.00"));
        assertThat(result.getProductDiscounts()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getFirstOrderDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getPromoDiscount()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getShipping()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getTax()).isEqualTo(new BigDecimal("84.00"));
        assertThat(result.getTotal()).isEqualTo(new BigDecimal("684.00"));
        assertThat(result.getIsFirstOrder()).isFalse();
        assertThat(result.getFreeShippingApplied()).isTrue();
        verify(userService).getUserByRequest(any());
        verify(cartService).getCartByUser(any());
        verify(pricingService).calculateCheckoutPreview(cart.getCartItems(), user, "Cairo");
    }
}