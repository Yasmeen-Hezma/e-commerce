package com.ecommerce.e_commerce.commerce.cart.service;

import com.ecommerce.e_commerce.commerce.cart.dto.CartItemRequest;
import com.ecommerce.e_commerce.commerce.cart.dto.CartItemResponse;
import com.ecommerce.e_commerce.commerce.cart.dto.CartResponse;
import com.ecommerce.e_commerce.commerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.e_commerce.commerce.cart.mapper.CartItemMapper;
import com.ecommerce.e_commerce.commerce.cart.mapper.CartMapper;
import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.cart.model.CartItemId;
import com.ecommerce.e_commerce.commerce.cart.repository.CartItemRepository;
import com.ecommerce.e_commerce.commerce.cart.repository.CartRepository;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.common.exception.EmptyCartException;
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.e_commerce.common.utils.Constants.CART_IS_EMPTY;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final CartItemMapper cartItemMapper;
    private final UserService userService;
    private final PricingService pricingService;

    private record CartItemResult(CartItem cartItem, boolean isNew) {
    }

    @Override
    @Transactional
    public CartItemResponse addItemToCart(HttpServletRequest request, CartItemRequest cartItemRequest) {
        Long userId = userService.getUserId(request);
        Cart cart = getOrCreateCart(userId);
        CartItemResult cartItemResult = findOrCreateCartItem(cart, cartItemRequest);
        updateCartItemQuantity(cartItemResult, cartItemRequest.getQuantity());
        saveCartAndItem(cart, cartItemResult.cartItem);
        return cartItemMapper.toResponse(cartItemResult.cartItem);
    }

    @Override
    public CartResponse getCartResponseByUser(HttpServletRequest request) {
        Cart cart = getCartByUser(request);
        PriceResult pricing = pricingService.calculateCart(cart.getCartItems());
        CartResponse response = cartMapper.toResponse(cart);
        BigDecimal totalDiscounts = pricing.productDiscount().add(pricing.firstOrderDiscount()).add(pricing.promoDiscount());
        response.setSubTotal(pricing.subtotal());
        response.setProductDiscounts(totalDiscounts);
        response.setTotalPrice(pricing.total());
        return response;
    }

    @Override
    @Transactional
    public CartResponse syncCartSnapshot(HttpServletRequest request, UpdateCartItemRequest updateRequest) {
        Cart cart = getCartByUser(request);
        List<StockWarning> warnings = new ArrayList<>();
        List<CartItem> updatedItems = buildMergedCartItems(updateRequest, cart, warnings);
        updateCart(updatedItems, cart);
        PriceResult pricing = pricingService.calculateCart(cart.getCartItems());
        CartResponse response = cartMapper.toResponseWithWarnings(cart, warnings);
        BigDecimal totalDiscounts = pricing.productDiscount().add(pricing.firstOrderDiscount()).add(pricing.promoDiscount());
        response.setSubTotal(pricing.subtotal());
        response.setProductDiscounts(totalDiscounts);
        response.setTotalPrice(pricing.total());
        return response;
    }

    @Override
    @Transactional
    public void clearCart(HttpServletRequest request) {
        Cart cart = getCartByUser(request);
        cart.clearItems();
    }

    @Override
    public void checkCartExisting(Cart cart) {
        if (cart.getCartItems().isEmpty()) {
            throw new EmptyCartException(CART_IS_EMPTY);
        }
    }

    private void updateCart(List<CartItem> updatedItems, Cart cart) {
        cart.getCartItems().clear();
        cart.setCartItems(updatedItems);
    }

    private List<CartItem> buildMergedCartItems(UpdateCartItemRequest updateRequest, Cart cart, List<StockWarning> warnings) {
        Map<Long, CartItem> existingItemsMap = buildExistingItemsMap(cart);
        removeUnwantedItems(updateRequest, existingItemsMap);
        mergeCartItems(updateRequest, cart, existingItemsMap, warnings);
        return new ArrayList<>(existingItemsMap.values());
    }

    private void removeUnwantedItems(UpdateCartItemRequest updateRequest, Map<Long, CartItem> existingItemsMap) {
        Set<Long> requestedIds = updateRequest
                .getCartItems()
                .stream()
                .map(CartItemRequest::getProductId)
                .collect(Collectors.toSet());
        existingItemsMap.entrySet().removeIf(entry -> !requestedIds.contains(entry.getKey()));
    }

    private Map<Long, CartItem> buildExistingItemsMap(Cart cart) {
        return cart.getCartItems()
                .stream()
                .collect(Collectors.toMap(item -> item.getProduct().getProductId(), item -> item));
    }

    private void mergeCartItems(UpdateCartItemRequest updateRequest, Cart cart, Map<Long, CartItem> existingItemsMap, List<StockWarning> warnings) {
        for (CartItemRequest item : updateRequest.getCartItems()) {
            Long productId = item.getProductId();
            int quantity = item.getQuantity();
            Product product = productService.getNonDeletedProductById(item.getProductId());
            Optional<StockWarning> warningOpt = productService.checkStockAndWarn(product, quantity);
            if (warningOpt.isPresent()) {
                warnings.add(warningOpt.get());
                continue;
            }
            if (existingItemsMap.containsKey(productId)) {
                existingItemsMap.get(productId).setQuantity(quantity);
            } else {
                CartItem newItem = buildCartItem(cart, product, quantity);
                existingItemsMap.put(productId, newItem);
            }
        }
    }

    private Cart getCartByUser(Long userId) {
        userService.getUserById(userId);
        return cartRepository.findByUser_UserId(userId).orElseGet(() -> createAndSaveNewCart(userId));
    }

    private Cart createAndSaveNewCart(Long userId) {
        User user = userService.getUserById(userId);
        Cart newCart = Cart.builder()
                .user(user)
                .cartItems(new ArrayList<>())
                .build();
        return cartRepository.save(newCart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUser_UserId(userId)
                .orElseGet(() -> createAndSaveNewCart(userId));
    }

    private CartItem createNewCartItem(Cart cart, CartItemRequest request) {
        Product product = productService.getNonDeletedProductById(request.getProductId());
        return buildCartItem(cart, product, request.getQuantity());
    }

    private CartItemId setCartItemId(Long cartId, Long productId) {
        CartItemId cartItemId = new CartItemId();
        cartItemId.setCart(cartId);
        cartItemId.setProduct(productId);
        return cartItemId;
    }

    private CartItemResult findOrCreateCartItem(Cart cart, CartItemRequest request) {
        return cart
                .getCartItems()
                .stream()
                .filter(item -> item.getProduct().getProductId().equals(request.getProductId()))
                .findFirst()
                .map(item -> new CartItemResult(item, false))
                .orElseGet(() -> new CartItemResult(createNewCartItem(cart, request), true));
    }

    private void updateCartItemQuantity(CartItemResult cartItemResult, int quantityToAdd) {
        if (cartItemResult.cartItem != null && !cartItemResult.isNew) {
            cartItemResult.cartItem.setQuantity(cartItemResult.cartItem.getQuantity() + quantityToAdd);
        }
    }

    @Override
    public Cart getCartByUser(HttpServletRequest request) {
        Long userId = userService.getUserId(request);
        return getCartByUser(userId);
    }

    private void saveCartAndItem(Cart cart, CartItem cartItem) {
        cartItemRepository.save(cartItem);
        cartRepository.save(cart);
    }

    private CartItem buildCartItem(Cart cart, Product product, int quantity) {
        CartItemId id = setCartItemId(cart.getCartId(), product.getProductId());
        return CartItem.builder()
                .id(id)
                .product(product)
                .cart(cart)
                .quantity(quantity)
                .priceSnapshot(pricingService.calculatePriceSnapshot(product))
                .build();
    }
}
