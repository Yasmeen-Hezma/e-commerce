package com.ecommerce.e_commerce.commerce.order.service;


import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.commerce.cart.service.CartService;
import com.ecommerce.e_commerce.commerce.pricing.model.PriceResult;
import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.common.exception.InvalidOperationException;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.common.exception.UnauthorizedException;
import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.dto.ShippingAddressRequest;
import com.ecommerce.e_commerce.commerce.order.dto.UserAddressDto;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.mapper.OrderMapper;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.model.OrderItem;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.ecommerce.e_commerce.common.utils.Constants.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final ProductService productService;
    private final UserService userService;
    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final OrderMapper orderMapper;
    private final PricingService pricingService;

    @Override
    @Transactional
    public OrderResponse createOrderFromCart(HttpServletRequest request, String promoCode, String governorate) {
        Cart cart = cartService.getCartByUser(request);
        cartService.checkCartExisting(cart);
        productService.checkStockAvailability(cart.getCartItems());
        User user = userService.getUserByRequest(request);
        PriceResult pricing = getPriceResult(promoCode, governorate, cart, user);
        Order order = buildOrder(user, pricing, promoCode);
        setOrderItemsToOrder(order, cart);
        Order savedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse addShippingAddress(Long orderId, ShippingAddressRequest addressRequest, HttpServletRequest request) {
        User user = userService.getUserByRequest(request);
        Order order = getOrderById(orderId);
        if (!user.getUserId().equals(order.getUser().getUserId())) {
            throw new UnauthorizedException(YOU_CAN_ONLY_ACCESS_YOUR_OWN_ORDERS);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOperationException(CANNOT_MODIFY_SHIPPING_ADDRESS_FOR_THIS_ORDER);
        }
        if (order.hasShippingAddress()) {
            throw new InvalidOperationException(ORDER_ALREADY_HAS_A_SHIPPING_ADDRESS);
        }
        if (Boolean.TRUE.equals(addressRequest.getUseDefaultAddress())) {
            UserAddressDto defaultAddress = userService.getUserDefaultAddress(request);
            setShippingAddressFromDto(order, defaultAddress);
        } else {
            validateAddressRequest(addressRequest);
            setShippingAddressFromRequest(order, addressRequest);
        }
        order.setCustomerFirstName(user.getFirstName());
        order.setCustomerLastName(user.getLastName());

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toOrderResponse(updatedOrder);
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository
                .findById(orderId)
                .orElseThrow(() -> new ItemNotFoundException(ORDER_NOT_FOUND));
    }

    @Override
    public OrderResponse getOrderResponseById(Long orderId) {
        Order order = getOrderById(orderId);
        return orderMapper.toOrderResponse(order);
    }

    private Order buildOrder(User user, PriceResult pricing, String promoCode) {
        return Order.builder()
                .user(user)
                .subtotal(pricing.subtotal())
                .productDiscounts(pricing.productDiscount())
                .tax(pricing.tax())
                .shipping(pricing.shipping())
                .firstOrderDiscount(pricing.firstOrderDiscount())
                .totalPrice(pricing.total())
                .status(OrderStatus.PENDING)
                .promoDiscount(pricing.promoDiscount())
                .promoCodeUsed(promoCode)
                .build();
    }

    private OrderItem convertToOrderItem(CartItem cartItem, Order order) {
        return OrderItem.builder()
                .order(order)
                .quantity(cartItem.getQuantity())
                .price(cartItem.getPriceSnapshot())
                .product(cartItem.getProduct())
                .build();
    }

    private List<OrderItem> convertCartItemsToOrderItems(Cart cart, Order order) {
        return cart.getCartItems().stream().map(cartItem -> convertToOrderItem(cartItem, order)).collect(Collectors.toList());
    }

    private void setOrderItemsToOrder(Order order, Cart cart) {
        List<OrderItem> orderItems = convertCartItemsToOrderItems(cart, order);
        order.setOrderItems(orderItems);
    }

    private BigDecimal calculateTotalPriceForOrder(List<OrderItem> orderItems) {
        return orderItems
                .stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PriceResult getPriceResult(String promoCode, String governorate, Cart cart, User user) {
        PriceResult pricing;
        if (promoCode != null) {
            pricing = pricingService.calculateWithPromo(cart.getCartItems(), user, promoCode, governorate);
        } else {
            pricing = pricingService.calculateCheckoutPreview(cart.getCartItems(), user, governorate);
        }
        return pricing;
    }

    private void setShippingAddressFromDto(Order order, UserAddressDto defaultAddress) {
        order.setShippingGovernorate(defaultAddress.getGovernorate());
        order.setShippingCity(defaultAddress.getCity());
        order.setShippingStreet(defaultAddress.getStreet());
        order.setShippingFloorNumber(defaultAddress.getFloorNumber());
        order.setShippingApartmentNumber(defaultAddress.getApartmentNumber());
        order.setShippingPhone(defaultAddress.getPhone());
    }

    private void setShippingAddressFromRequest(Order order, ShippingAddressRequest addressRequest) {
        order.setShippingGovernorate(addressRequest.getGovernorate());
        order.setShippingCity(addressRequest.getCity());
        order.setShippingStreet(addressRequest.getStreet());
        order.setShippingFloorNumber(addressRequest.getFloorNumber());
        order.setShippingApartmentNumber(addressRequest.getApartmentNumber());
        order.setShippingPhone(addressRequest.getPhone());
        order.setDeliveryNotes(addressRequest.getDeliveryNotes());
    }

    private void validateAddressRequest(ShippingAddressRequest request) {
        if (request.getGovernorate() == null || request.getGovernorate().isBlank()) {
            throw new InvalidOperationException(GOVERNORATE_IS_REQUIRED);
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new InvalidOperationException(CITY_IS_REQUIRED);
        }
        if (request.getStreet() == null || request.getStreet().isBlank()) {
            throw new InvalidOperationException(STREET_IS_REQUIRED);
        }
        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new InvalidOperationException(PHONE_IS_REQUIRED);
        }
    }
}
