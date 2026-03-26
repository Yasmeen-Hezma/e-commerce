package com.ecommerce.e_commerce.commerce.cart.service;

import com.ecommerce.e_commerce.commerce.cart.dto.CartItemRequest;
import com.ecommerce.e_commerce.commerce.cart.dto.CartItemResponse;
import com.ecommerce.e_commerce.commerce.cart.dto.CartResponse;
import com.ecommerce.e_commerce.commerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import jakarta.servlet.http.HttpServletRequest;

public interface CartService {
    CartItemResponse addItemToCart(HttpServletRequest request, CartItemRequest cartItemRequest);

    CartResponse getCartResponseByUser(HttpServletRequest request);

    Cart getCartByUser(HttpServletRequest request);

    CartResponse syncCartSnapshot(HttpServletRequest request, UpdateCartItemRequest updateRequest);

    void clearCart(HttpServletRequest request);

    void checkCartExisting(Cart cart);

    Cart getCartByUserId(Long userId);
}
