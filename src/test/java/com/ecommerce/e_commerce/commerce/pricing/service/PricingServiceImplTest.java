package com.ecommerce.e_commerce.commerce.pricing.service;

import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.common.exception.InvalidOperationException;
import com.ecommerce.e_commerce.user.profile.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceImplTest {
    private PricingServiceImpl pricingService;
    private List<CartItem> cartItems;
    private User user;

    @BeforeEach
    void setUp() {
        pricingService = new PricingServiceImpl();
        user = User
                .builder()
                .userId(1L)
                .orders(new ArrayList<>())
                .build();
        cartItems = createCartItems();
    }

    @Test
    void calculatePriceSnapshot_ShouldCalculateCorrectly_WhenDiscountExists() {
        // Arrange
        Product product = Product
                .builder()
                .price(new BigDecimal("100.00"))
                .discount(new BigDecimal("10.00"))
                .build();
        // Act
        BigDecimal result = pricingService.calculatePriceSnapshot(product);
        // Assert
        assertEquals(new BigDecimal("90.00"), result);
    }

    @Test
    void calculatePriceSnapshot_ShouldReturnOriginalPrice_WhenDiscountDoesNotExist() {
        // Arrange
        Product product = Product
                .builder()
                .price(new BigDecimal("100.00"))
                .build();
        // Act
        BigDecimal result = pricingService.calculatePriceSnapshot(product);
        // Assert
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void calculateCart_ShouldCalculateCorrectly_WhenProductDiscountsExist() {
        // Arrange
        Product product1 = Product
                .builder()
                .price(new BigDecimal("100.00"))
                .discount(new BigDecimal("10.00"))
                .build();
        Product product2 = Product
                .builder()
                .price(new BigDecimal("100.00"))
                .build();
        CartItem item1 = CartItem
                .builder()
                .product(product1)
                .quantity(1)
                .build();
        CartItem item2 = CartItem
                .builder()
                .product(product2)
                .quantity(2)
                .build();
        List<CartItem> cartItems = List.of(item1, item2);
        // Act
        PriceResult result = pricingService.calculateCart(cartItems);
        // Assert
        assertEquals(new BigDecimal("300.00"), result.subtotal());
        assertEquals(new BigDecimal("10.00"), result.productDiscount());
        assertEquals(new BigDecimal("290.00"), result.total());
    }

    @Test
    void calculateCheckoutPreview_ShouldApplyFirstOrderDiscount_WhenFirstOrder() {
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(new BigDecimal("50.00"), result.firstOrderDiscount());
    }

    @Test
    void calculateCheckoutPreview_ShouldNotApplyFirstOrderDiscount_WhenNotFirstOrder() {
        // Arrange
        Order completedOrder = Order
                .builder()
                .status(OrderStatus.CONFIRMED)
                .build();
        user.setOrders(List.of(completedOrder));
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(BigDecimal.ZERO, result.firstOrderDiscount());
    }

    @Test
    void calculateCheckoutPreview_ShouldApplyFreeShipping_WhenAboveThreshold() {
        // Arrange
        cartItems = createExpensiveCart();
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(BigDecimal.ZERO, result.shipping());
        assertEquals(new BigDecimal("600.00"), result.subtotal());
    }

    @Test
    void calculateCheckoutPreview_ShouldApplyCairoShipping_WhenBelowThreshold() {
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(new BigDecimal("30.00"), result.shipping());
        assertEquals(new BigDecimal("100.00"), result.subtotal());
    }

    @Test
    void calculateCheckoutPreview_ShouldApplyAlexandriaShipping_WhenBelowThreshold() {
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Alexandria");
        // Assert
        assertEquals(new BigDecimal("40.00"), result.shipping());
        assertEquals(new BigDecimal("100.00"), result.subtotal());
    }

    @Test
    void applyPromoCode_ShouldApplyDiscount_WhenValidPromoCode() {
        // Act
        PriceResult result = pricingService.calculateWithPromo(cartItems, user, "SAVE20", "Cairo");
        // Assert
        assertEquals(new BigDecimal("100.00"), result.subtotal());

        BigDecimal base = result.subtotal()
                .subtract(result.firstOrderDiscount())
                .subtract(result.productDiscount());

        assertTrue(result.promoDiscount().compareTo(BigDecimal.ZERO) > 0);

        assertEquals(
                0,
                result.promoDiscount().compareTo(base.multiply(new BigDecimal("0.20")))
        );
    }

    @Test
    void applyPromoCode_ShouldThrowException_WhenInvalidPromoCode() {
        // Act & Assert
        assertThrows(InvalidOperationException.class,
                () -> {
                    pricingService.calculateWithPromo(cartItems, user, "INVALID_CODE", "Cairo");
                });
    }

    @Test
    void calculateCheckoutPreview_ShouldApply14PercentTax_WhenDefaultTax() {
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(new BigDecimal("100.00"), result.subtotal());

        BigDecimal base = result.subtotal()
                .subtract(result.firstOrderDiscount())
                .subtract(result.productDiscount());

        assertTrue(result.tax().compareTo(BigDecimal.ZERO) > 0);

        assertEquals(
                0,
                result.tax().compareTo(base.multiply(new BigDecimal("0.14")))
        );
    }

    @Test
    void calculateCheckoutPreview_ShouldCalculateCorrectly_WhenFullFlow() {
        // Act
        PriceResult result = pricingService.calculateCheckoutPreview(cartItems, user, "Cairo");
        // Assert
        assertEquals(new BigDecimal("100.00"), result.subtotal());
        assertEquals(new BigDecimal("10.00"), result.productDiscount());
        assertEquals(new BigDecimal("50.00"), result.firstOrderDiscount());
        assertEquals(new BigDecimal("30.00"), result.shipping());
        assertTrue(result.tax().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.total().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void applyPromoCode_ShouldCalculateCorrectly_WhenFullFlow() {
        // Act
        PriceResult result = pricingService.calculateWithPromo(cartItems, user, "SAVE20","Cairo");
        // Assert
        assertEquals(new BigDecimal("100.00"), result.subtotal());
        assertEquals(new BigDecimal("10.00"), result.productDiscount());
        assertEquals(new BigDecimal("50.00"), result.firstOrderDiscount());
        assertEquals(new BigDecimal("30.00"), result.shipping());
        assertTrue(result.tax().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.promoDiscount().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.total().compareTo(BigDecimal.ZERO) > 0);
    }


    private List<CartItem> createCartItems() {
        Product product = Product
                .builder()
                .price(new BigDecimal("100.00"))
                .discount(new BigDecimal("10.00"))
                .build();
        CartItem item = CartItem
                .builder()
                .product(product)
                .quantity(1)
                .build();
        return List.of(item);
    }

    private List<CartItem> createExpensiveCart() {
        Product product = Product
                .builder()
                .price(new BigDecimal("600.00"))
                .build();
        CartItem item = CartItem
                .builder()
                .product(product)
                .quantity(1)
                .build();
        return List.of(item);
    }
}