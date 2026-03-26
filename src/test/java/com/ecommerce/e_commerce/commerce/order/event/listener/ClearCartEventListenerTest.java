package com.ecommerce.e_commerce.commerce.order.event.listener;

import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.cart.service.CartService;
import com.ecommerce.e_commerce.commerce.order.event.OrderCompletedEvent;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.model.OrderItem;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.user.profile.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearCartEventListenerTest {
    @Mock
    private CartService cartService;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private ClearCartEventListener listener;

    @Test
    void handleOrderCompleted_ShouldClearCart_WhenOrderCompleted() {
        // Arrange
        Product product = Product
                .builder()
                .productId(1L)
                .build();
        CartItem cartItem = CartItem
                .builder()
                .product(product)
                .quantity(2)
                .build();
        OrderItem orderItem = OrderItem
                .builder()
                .product(product)
                .quantity(2)
                .build();
        Cart cart = Cart
                .builder()
                .cartId(1L)
                .cartItems(new ArrayList<>(List.of(cartItem)))
                .build();
        Order order = Order
                .builder()
                .orderId(1L)
                .orderItems(List.of(orderItem))
                .build();
        User user = User
                .builder()
                .userId(1L)
                .build();
        order.setUser(user);
        OrderCompletedEvent event =
                new OrderCompletedEvent(1L, BigDecimal.valueOf(100), "test@email.com");

        when(orderService.getOrderById(1L)).thenReturn(order);
        when(cartService.getCartByUserId(order.getUser().getUserId())).thenReturn(cart);
        // Act
        listener.handleOrderCompleted(event);
        // Assert
        verify(orderService).getOrderById(1L);
        verify(cartService).getCartByUserId(order.getUser().getUserId());
        assertThat(cart.getCartItems()).isEmpty();
    }
}