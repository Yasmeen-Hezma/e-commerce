package com.ecommerce.e_commerce.commerce.order.service;

import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.dto.ShippingAddressRequest;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import jakarta.servlet.http.HttpServletRequest;

public interface OrderService {
    OrderResponse createOrderFromCart(HttpServletRequest request, String promoCode, String governorate);

    OrderResponse addShippingAddress(Long orderId, ShippingAddressRequest addressRequest, HttpServletRequest request);

    Order getOrderById(Long orderId);

    OrderResponse getOrderResponseById(Long orderId);

}
