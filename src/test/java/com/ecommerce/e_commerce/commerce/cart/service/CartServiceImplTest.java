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
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import com.ecommerce.e_commerce.commerce.product.enums.StockWarningType;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
import com.ecommerce.e_commerce.common.exception.EmptyCartException;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static com.ecommerce.e_commerce.common.utils.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock
    private CartMapper cartMapper;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductService productService;
    @Mock
    private CartItemMapper cartItemMapper;
    @Mock
    private UserService userService;
    @Mock
    private PricingService pricingService;
    @Mock
    private HttpServletRequest httpRequest;
    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private CartItem cartItem;
    private CartItemRequest cartItemRequest;
    private CartItemResponse cartItemResponse;
    private CartResponse cartResponse;
    private PriceResult priceResult;

    @BeforeEach
    void setUp() {
        user = User
                .builder()
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .phoneCode(20)
                .orders(new ArrayList<>())
                .build();

        product = Product
                .builder()
                .productId(100L)
                .productName("test product")
                .description("test description")
                .quantity(50)
                .price(BigDecimal.valueOf(99.99))
                .discount(BigDecimal.TEN)
                .deleted(false)
                .cartItems(new ArrayList<>())
                .orderItems(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        cart = Cart
                .builder()
                .cartId(1L)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        cartItem = CartItem
                .builder()
                .cart(cart)
                .product(product)
                .quantity(2)
                .priceSnapshot(BigDecimal.valueOf(89.99))
                .build();

        cartItemRequest = new CartItemRequest();
        cartItemRequest.setProductId(100L);
        cartItemRequest.setQuantity(2);

        cartItemResponse = CartItemResponse
                .builder()
                .productId(100L)
                .productName("test product")
                .quantity(2)
                .originalPrice(BigDecimal.valueOf(99.99))
                .priceSnapshot(BigDecimal.valueOf(89.99))
                .discountPercent(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(179.98))
                .build();
        cartResponse = CartResponse
                .builder()
                .id(1L)
                .userId(1L)
                .items(List.of(cartItemResponse))
                .subTotal(BigDecimal.valueOf(199.98))
                .productDiscounts(BigDecimal.valueOf(19.98))
                .totalPrice(BigDecimal.valueOf(180))
                .build();
        priceResult = PriceResult
                .builder()
                .subtotal(BigDecimal.valueOf(199.98))
                .productDiscount(BigDecimal.valueOf(19.98))
                .total(BigDecimal.valueOf(180))
                .shipping(BigDecimal.ZERO)
                .tax(BigDecimal.ZERO)
                .firstOrderDiscount(BigDecimal.ZERO)
                .promoDiscount(BigDecimal.ZERO)
                .build();
    }

    @Test
    void addItemToCart_ShouldAddNewItem_WhenItemDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemMapper.toResponse(any(CartItem.class))).thenReturn(cartItemResponse);
        // Act
        CartItemResponse result = cartService.addItemToCart(httpRequest, cartItemRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(cart);
        verify(cartItemMapper).toResponse(any(CartItem.class));
    }

    @Test
    void addItemToCart_ShouldIncrementQuantity_WhenItemAlreadyExists() {
        // Arrange
        cart.addCartItem(cartItem);

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemMapper.toResponse(any(CartItem.class))).thenAnswer(
                invocation -> {
                    CartItem item = invocation.getArgument(0);
                    return CartItemResponse
                            .builder()
                            .productId(item.getProduct().getProductId())
                            .quantity(item.getQuantity())
                            .build();
                }
        );
        cartItemRequest.setQuantity(3);
        // Act
        CartItemResponse result = cartService.addItemToCart(httpRequest, cartItemRequest);
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(cart);
        verify(cartItemMapper).toResponse(any(CartItem.class));
    }

    @Test
    void addItemToCart_ShouldCreateCart_WhenCartDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartItemMapper.toResponse(any(CartItem.class))).thenReturn(cartItemResponse);
        // Act
        CartItemResponse result = cartService.addItemToCart(httpRequest, cartItemRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository, times(2)).save(any(Cart.class));
        verify(cartItemMapper).toResponse(any(CartItem.class));
    }

    @Test
    void addItemToCart_ShouldThrowException_WhenProductNotFound() {
        // Arrange
        cartItemRequest.setProductId(999L);
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getNonDeletedProductById(999L))
                .thenThrow(new ItemNotFoundException(PRODUCT_NOT_FOUND));
        // Act & Assert
        assertThatThrownBy(() -> cartService.addItemToCart(httpRequest, cartItemRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(PRODUCT_NOT_FOUND);
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(999L);
        verify(cartItemRepository, never()).save(any(CartItem.class));
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getCartResponseByUser_ShouldReturnCart_WhenCartExists() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(pricingService.calculateCart(cart.getCartItems())).thenReturn(priceResult);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);
        // Act
        CartResponse result = cartService.getCartResponseByUser(httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(180));
        assertThat(result.getProductDiscounts()).isEqualTo(BigDecimal.valueOf(19.98));
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(pricingService).calculateCart(cart.getCartItems());
        verify(cartMapper).toResponse(cart);
    }

    @Test
    void getCartResponseByUser_ShouldCreateCart_WhenCartDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(pricingService.calculateCart(cart.getCartItems())).thenReturn(priceResult);
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(cartResponse);
        // Act
        CartResponse result = cartService.getCartResponseByUser(httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getTotalPrice()).isEqualTo(BigDecimal.valueOf(180));
        assertThat(result.getProductDiscounts()).isEqualTo(BigDecimal.valueOf(19.98));
        verify(userService).getUserId(httpRequest);
        verify(cartRepository).findByUser_UserId(1L);
        verify(userService, times(2)).getUserById(1L);
        verify(cartRepository).save(any(Cart.class));
        verify(pricingService).calculateCart(cart.getCartItems());
        verify(cartMapper).toResponse(any(Cart.class));
    }

    @Test
    void getCartResponseByUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L))
                .thenThrow(new ItemNotFoundException(USER_NOT_FOUND));
        // Act & Assert
        assertThatThrownBy(() -> cartService.getCartResponseByUser(httpRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(USER_NOT_FOUND);
        verify(userService).getUserId(httpRequest);
    }

    @Test
    void syncCartSnapshot_ShouldUpdateQuantities_WhenAllProductsAvailable() {
        // Arrange
        cart.addCartItem(cartItem);

        CartItemRequest cartItemRequest2 = new CartItemRequest();
        cartItemRequest2.setProductId(100L);
        cartItemRequest2.setQuantity(5);

        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest();
        updateRequest.setCartItems(List.of(cartItemRequest2));

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 5)).thenReturn(Optional.empty());
        when(pricingService.calculateCart(cart.getCartItems())).thenReturn(priceResult);
        when(cartMapper.toResponseWithWarnings(any(Cart.class), anyList()))
                .thenAnswer(invocation -> {
                    Cart c = invocation.getArgument(0);
                    return CartResponse
                            .builder()
                            .items(c.getCartItems()
                                    .stream()
                                    .map(item -> CartItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .build();
                });
        // Act
        CartResponse result = cartService.syncCartSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(5);
        verify(userService).getUserId(httpRequest);
        verify(userService).getUserById(1L);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 5);
        verify(pricingService).calculateCart(cart.getCartItems());
        verify(cartMapper).toResponseWithWarnings(any(Cart.class), anyList());
    }

    @Test
    void syncCartSnapshot_ShouldReturnWarnings_WhenStockInsufficient() {
        // Arrange
        cart.addCartItem(cartItem);

        CartItemRequest cartItemRequest2 = new CartItemRequest();
        cartItemRequest2.setProductId(100L);
        cartItemRequest2.setQuantity(500);

        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest();
        updateRequest.setCartItems(List.of(cartItemRequest2));
        StockWarning warning = StockWarning
                .builder()
                .productId(100L)
                .type(StockWarningType.LIMITED_STOCK)
                .build();
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 500)).thenReturn(Optional.of(warning));
        when(pricingService.calculateCart(cart.getCartItems())).thenReturn(priceResult);
        when(cartMapper.toResponseWithWarnings(any(Cart.class), anyList()))
                .thenAnswer(invocation -> {
                    Cart c = invocation.getArgument(0);
                    List<StockWarning> warnings = invocation.getArgument(1);
                    return CartResponse
                            .builder()
                            .items(c.getCartItems()
                                    .stream()
                                    .map(item -> CartItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .warnings(warnings)
                            .build();
                });
        // Act
        CartResponse result = cartService.syncCartSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(2); // previous value before warnings
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().getFirst().getProductId()).isEqualTo(100L);
        verify(userService).getUserId(httpRequest);
        verify(userService).getUserById(1L);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 500);
        verify(pricingService).calculateCart(cart.getCartItems());
        verify(cartMapper).toResponseWithWarnings(any(Cart.class), anyList());
    }

    @Test
    void syncCartSnapshot_ShouldRemoveItems_WhenNotInUpdateRequest() {
        // Arrange
        CartItem cartItem2 = CartItem
                .builder()
                .id(new CartItemId())
                .cart(cart)
                .product(Product.builder().productId(200L).price(BigDecimal.TEN).build())
                .quantity(2)
                .priceSnapshot(BigDecimal.TEN)
                .build();
        cart.addCartItem(cartItem);
        cart.addCartItem(cartItem2);
        // Update request only includes product 100, not 200
        CartItemRequest cartItemRequest2 = new CartItemRequest();
        cartItemRequest2.setProductId(100L);
        cartItemRequest2.setQuantity(3);
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest();
        updateRequest.setCartItems(List.of(cartItemRequest2));

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 3)).thenReturn(Optional.empty());
        when(pricingService.calculateCart(cart.getCartItems())).thenReturn(priceResult);
        when(cartMapper.toResponseWithWarnings(any(Cart.class), anyList()))
                .thenAnswer(invocation -> {
                    Cart c = invocation.getArgument(0);
                    return CartResponse
                            .builder()
                            .items(c.getCartItems()
                                    .stream()
                                    .map(item -> CartItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .build();
                });
        // Act
        CartResponse result = cartService.syncCartSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(3);
        verify(userService).getUserId(httpRequest);
        verify(userService).getUserById(1L);
        verify(cartRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 3);
        verify(pricingService).calculateCart(cart.getCartItems());
        verify(cartMapper).toResponseWithWarnings(any(Cart.class), anyList());
    }

    @Test
    void clearCart_ShouldRemoveAllItems_WhenCartHasItems() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.of(cart));
        // Act
        cartService.clearCart(httpRequest);
        // Assert
        assertThat(cart.getCartItems()).isEmpty();
        verify(userService).getUserId(httpRequest);
        verify(userService).getUserById(1L);
        verify(cartRepository).findByUser_UserId(1L);
    }

    @Test
    void checkCartExisting_ShouldThrowException_WhenCartIsEmpty() {
        // Arrange
        // cart has no items
        // Act & Assert
        assertThatThrownBy(() -> cartService.checkCartExisting(cart))
                .isInstanceOf(EmptyCartException.class)
                .hasMessageContaining(CART_IS_EMPTY);
    }

    @Test
    void getOrCreateCart_ShouldCreateNewCart_WhenCartDoesNotExist() {
        // Arrange
        when(cartRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(userService.getUserById(1L)).thenReturn(user);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartItemMapper.toResponse(any(CartItem.class))).thenReturn(cartItemResponse);
        // Act
        cartService.addItemToCart(httpRequest, cartItemRequest);
        // Assert
        verify(userService).getUserById(1L);
        verify(cartRepository, times(2)).save(any(Cart.class));
        verify(userService).getUserId(httpRequest);
        verify(productService).getNonDeletedProductById(100L);
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartItemMapper).toResponse(any(CartItem.class));
    }
}