package com.ecommerce.e_commerce.commerce.order.service;

import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.cart.model.CartItemId;
import com.ecommerce.e_commerce.commerce.cart.service.CartService;
import com.ecommerce.e_commerce.commerce.order.dto.OrderItemResponse;
import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.dto.ShippingAddressRequest;
import com.ecommerce.e_commerce.commerce.order.dto.UserAddressDto;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.mapper.OrderMapper;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.commerce.product.enums.ProductStatus;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
import com.ecommerce.e_commerce.common.exception.EmptyCartException;
import com.ecommerce.e_commerce.common.exception.InvalidOperationException;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.common.exception.UnauthorizedException;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.method.P;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ecommerce.e_commerce.common.utils.Constants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    private ProductService productService;
    @Mock
    private UserService userService;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartService cartService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private PricingService pricingService;
    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private Order order;
    private Cart cart;
    private OrderResponse orderResponse;
    private UserAddressDto addressDto;
    private ShippingAddressRequest shippingAddressRequest;
    private PriceResult priceResult;

    @BeforeEach
    void setUp() {
        Product product = Product
                .builder()
                .productId(100L)
                .productName("test product")
                .description("test description")
                .price(BigDecimal.valueOf(99.99))
                .quantity(10)
                .status(ProductStatus.AVAILABLE)
                .reviewCount(0)
                .discount(BigDecimal.ZERO)
                .averageRating(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .cartItems(new ArrayList<>())
                .reviews(new ArrayList<>())
                .deleted(false)
                .build();

        user = User.builder()
                .userId(1L)
                .firstName("Yousef")
                .lastName("Mahmoud")
                .phone("01098765432")
                .phoneCode(20)
                .governorate("Cairo")
                .city("Cairo")
                .street("Street 1")
                .floorNumber("3")
                .apartmentNumber("12B")
                .build();

        cart = Cart
                .builder()
                .cartId(1L)
                .user(user)
                .cartItems(new ArrayList<>())
                .build();

        CartItemId cartItemId = new CartItemId();
        cartItemId.setProduct(100L);
        cartItemId.setCart(1L);

        CartItem cartItem = CartItem
                .builder()
                .id(cartItemId)
                .cart(cart)
                .product(product)
                .quantity(2)
                .priceSnapshot(BigDecimal.valueOf(99.99))
                .build();

        cart.getCartItems().add(cartItem);

        order = Order
                .builder()
                .orderId(1L)
                .user(user)
                .status(OrderStatus.PENDING)
                .orderItems(new ArrayList<>())
                .totalPrice(BigDecimal.valueOf(257.98))
                .build();
        priceResult = PriceResult
                .builder()
                .subtotal(BigDecimal.valueOf(199.98))
                .shipping(BigDecimal.valueOf(30.00))
                .tax(BigDecimal.valueOf(0.14))
                .firstOrderDiscount(BigDecimal.ZERO)
                .productDiscount(BigDecimal.ZERO)
                .promoDiscount(BigDecimal.ZERO)
                .total(BigDecimal.valueOf(257.98))
                .build();

        OrderItemResponse orderItemResponse = OrderItemResponse
                .builder()
                .productId(100L)
                .price(BigDecimal.valueOf(99.99))
                .quantity(2)
                .image("product.png")
                .productName("test product")
                .build();

        shippingAddressRequest = ShippingAddressRequest.builder()
                .useDefaultAddress(false)
                .governorate("Giza")
                .city("6th October")
                .street("Street 10")
                .floorNumber("3")
                .apartmentNumber("5")
                .phone("9876543210")
                .deliveryNotes("Ring the bell")
                .build();

        addressDto = UserAddressDto.builder()
                .phone("01098765432")
                .governorate("Cairo")
                .city("Cairo")
                .street("Street 1")
                .floorNumber("3")
                .apartmentNumber("12B")
                .build();
        orderResponse = OrderResponse.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .address(addressDto)
                .items(List.of(orderItemResponse))
                .totalPrice(BigDecimal.valueOf(199.98))
                .build();
    }

    @Test
    void createOrderFromCart_ShouldCreateOrder_WhenValidRequest() {
        // Arrange
        when(cartService.getCartByUser(httpRequest)).thenReturn(cart);
        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(pricingService.calculateCheckoutPreview(cart.getCartItems(), user, "Cairo")).thenReturn(priceResult);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            savedOrder.setOrderId(1L);
            assertThat(savedOrder.getUser()).isEqualTo(user);
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(savedOrder.getOrderItems()).hasSize(1);
            assertThat(savedOrder.getTotalPrice()).isEqualTo(BigDecimal.valueOf(257.98));
            return savedOrder;
        });
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(orderResponse);
        // Act
        OrderResponse result = orderService.createOrderFromCart(httpRequest, null, "Cairo");
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(cartService).getCartByUser(httpRequest);
        verify(cartService).checkCartExisting(cart);
        verify(productService).checkStockAvailability(cart.getCartItems());
        verify(userService).getUserByRequest(httpRequest);
        verify(pricingService).calculateCheckoutPreview(cart.getCartItems(), user, "Cairo");
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toOrderResponse(any(Order.class));
    }

    @Test
    void createOrderFromCart_ShouldThrowException_WhenCartIsEmpty() {
        // Arrange
        when(cartService.getCartByUser(httpRequest)).thenReturn(cart);
        doThrow(new EmptyCartException(CART_IS_EMPTY))
                .when(cartService).checkCartExisting(cart);
        // Act & Assert
        assertThatThrownBy(() -> orderService.createOrderFromCart(httpRequest, null, "Cairo"))
                .isInstanceOf(EmptyCartException.class)
                .hasMessageContaining(CART_IS_EMPTY);
        verify(cartService).getCartByUser(httpRequest);
        verify(cartService).checkCartExisting(cart);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldAddCustomAddress_WhenUseDefaultIsFalse() {
        // Arrange
        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            assertThat(savedOrder.getShippingGovernorate()).isEqualTo("Giza");
            assertThat(savedOrder.getShippingCity()).isEqualTo("6th October");
            assertThat(savedOrder.getShippingStreet()).isEqualTo("Street 10");
            assertThat(savedOrder.getShippingApartmentNumber()).isEqualTo("5");
            assertThat(savedOrder.getShippingFloorNumber()).isEqualTo("3");
            assertThat(savedOrder.getShippingPhone()).isEqualTo("9876543210");
            assertThat(savedOrder.getDeliveryNotes()).isEqualTo("Ring the bell");
            return savedOrder;
        });
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(orderResponse);
        // Act
        OrderResponse result = orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toOrderResponse(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldUseDefaultAddress_WhenUseDefaultIsTrue() {
        // Arrange
        shippingAddressRequest.setUseDefaultAddress(true);

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userService.getUserDefaultAddress(httpRequest)).thenReturn(addressDto);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order savedOrder = invocation.getArgument(0);
            assertThat(savedOrder.getShippingGovernorate()).isEqualTo("Cairo");
            assertThat(savedOrder.getShippingCity()).isEqualTo("Cairo");
            assertThat(savedOrder.getShippingStreet()).isEqualTo("Street 1");
            assertThat(savedOrder.getShippingApartmentNumber()).isEqualTo("12B");
            assertThat(savedOrder.getShippingFloorNumber()).isEqualTo("3");
            assertThat(savedOrder.getShippingPhone()).isEqualTo("01098765432");
            return savedOrder;
        });
        when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(orderResponse);
        // Act
        OrderResponse result = orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(any(Order.class));
        verify(orderMapper).toOrderResponse(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        doThrow(new ItemNotFoundException(USER_NOT_FOUND))
                .when(userService).getUserByRequest(httpRequest);
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(USER_NOT_FOUND);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenUserNotOrderOwner() {
        // Arrange
        User differentUser = User
                .builder()
                .userId(99L)
                .firstName("John")
                .lastName("Doe")
                .build();
        when(userService.getUserByRequest(httpRequest)).thenReturn(differentUser);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining(YOU_CAN_ONLY_ACCESS_YOUR_OWN_ORDERS);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenOrderNotPending() {
        // Arrange
        order.setStatus(OrderStatus.PAYMENT_FAILED);
        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(CANNOT_MODIFY_SHIPPING_ADDRESS_FOR_THIS_ORDER);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenAddressAlreadyExist() {
        // Arrange
        order.setShippingGovernorate("Cairo");
        order.setShippingCity("Cairo");
        order.setShippingStreet("1");
        order.setShippingPhone("1234567891");

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(ORDER_ALREADY_HAS_A_SHIPPING_ADDRESS);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenGovernmentIsMissing() {
        // Arrange
        shippingAddressRequest.setGovernorate(null);

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(GOVERNORATE_IS_REQUIRED);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenCityIsMissing() {
        // Arrange
        shippingAddressRequest.setCity(null);

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(CITY_IS_REQUIRED);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenStreetIsMissing() {
        // Arrange
        shippingAddressRequest.setStreet(null);

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(STREET_IS_REQUIRED);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void setShippingAddress_ShouldThrowException_WhenPhoneIsMissing() {
        // Arrange
        shippingAddressRequest.setPhone(null);

        when(userService.getUserByRequest(httpRequest)).thenReturn(user);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act & Assert
        assertThatThrownBy(() -> orderService.addShippingAddress(1L, shippingAddressRequest, httpRequest))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining(PHONE_IS_REQUIRED);
        verify(userService).getUserByRequest(httpRequest);
        verify(orderRepository).findById(1L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrder_WhenOrderExists() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        // Act
        Order result = orderService.getOrderById(1L);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderById_ShouldThrowException_WhenOrderNotExist() {
        // Arrange
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(ORDER_NOT_FOUND);
        // Assert
        verify(orderRepository).findById(99L);
    }

    @Test
    void getOrderResponseById_ShouldReturnOrderResponse_WhenOrderExists() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        // Act
        OrderResponse result = orderService.getOrderResponseById(1L);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderRepository).findById(1L);
        verify(orderMapper).toOrderResponse(order);
    }
}