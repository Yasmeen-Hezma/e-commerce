package com.ecommerce.e_commerce.commerce.order.event.listener;

import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.service.CartService;
import com.ecommerce.e_commerce.commerce.order.event.OrderCompletedEvent;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClearCartEventListener {
    private final OrderService orderService;
    private final CartService cartService;

    @EventListener
    @Transactional
    public void handleOrderCompleted(OrderCompletedEvent event) {
        Order order = orderService.getOrderById(event.orderId());
        Cart cart = cartService.getCartByUserId(order.getUser().getUserId());
        cart.clearItems();
    }
}
