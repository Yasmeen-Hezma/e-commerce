package com.ecommerce.e_commerce.commerce.payment.strategy.offline;

import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.event.OrderCompletedEvent;
import com.ecommerce.e_commerce.commerce.order.event.listener.InventoryUpdateEventListener;
import com.ecommerce.e_commerce.commerce.order.event.listener.OrderConfirmationEmailEventListener;
import com.ecommerce.e_commerce.commerce.order.mapper.OrderMapper;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentMethod;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentStatus;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.commerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidationChain;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidator;
import com.ecommerce.e_commerce.common.exception.InvalidOrderStatusException;
import com.ecommerce.e_commerce.common.exception.InvalidOrderTotalException;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.common.exception.PaymentAlreadyCompletedException;
import com.ecommerce.e_commerce.security.auth.model.AuthUser;
import com.ecommerce.e_commerce.user.profile.model.User;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ecommerce.e_commerce.common.utils.Constants.*;
import static com.ecommerce.e_commerce.common.utils.Constants.PAYMENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CODPaymentStrategyTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentOrderValidationChain paymentOrderValidationChain;
    @Mock
    private PaymentOrderValidator paymentOrderValidator;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @InjectMocks
    private CODPaymentStrategy CODPaymentStrategy;

    private Order order;
    private PaymentTransaction paymentTransaction;

    @BeforeEach
    void setUp() {
        paymentTransaction = PaymentTransaction
                .builder()
                .paymentId(1L)
                .order(order)
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.PENDING)
                .paypalOrderId("PAYPAL-ORDER-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .build();
        User user = User.builder()
                .userId(1L)
                .firstName("Yousef")
                .lastName("Mahmoud")
                .build();
        AuthUser authUser = AuthUser
                .builder()
                .userId(1L)
                .user(user)
                .email("Yousef@email.com")
                .build();
        user.setAuthUser(authUser);
        order = Order
                .builder()
                .orderId(1L)
                .totalPrice(BigDecimal.valueOf(100.00))
                .status(OrderStatus.PENDING)
                .user(user)
                .build();
    }

    @Test
    void createCODPayment_ShouldCreateCODPayment_WhenValidRequest() {
        // Arrange
        OrderResponse orderResponse = OrderResponse
                .builder()
                .id(1L)
                .build();
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        when(paymentTransactionRepository.save(any(PaymentTransaction.class))).thenReturn(paymentTransaction);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toOrderResponse(order)).thenReturn(orderResponse);
        // Act
        OrderResponse result = CODPaymentStrategy.createPayment(1L);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).save(argThat(pt -> pt.getPaymentStatus() == PaymentStatus.PENDING && pt.getPaymentMethod() == PaymentMethod.COD));
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(orderMapper).toOrderResponse(order);
    }

    @Test
    void createCODPayment_ShouldThrowException_WhenOrderAlreadyCompleted() {
        // Arrange
        paymentTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaymentTransaction(paymentTransaction);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new PaymentAlreadyCompletedException(ORDER_ALREADY_COMPLETED))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Assert & Act
        assertThatThrownBy(() -> CODPaymentStrategy.createPayment(1L))
                .isInstanceOf(PaymentAlreadyCompletedException.class)
                .hasMessageContaining(ORDER_ALREADY_COMPLETED);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createCODPayment_ShouldThrowException_WhenInValidOrderTotal() {
        // Arrange
        order.setTotalPrice(BigDecimal.ZERO);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new InvalidOrderTotalException(INVALID_ORDER_TOTAL))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Assert & Act
        assertThatThrownBy(() -> CODPaymentStrategy.createPayment(1L))
                .isInstanceOf(InvalidOrderTotalException.class)
                .hasMessageContaining(INVALID_ORDER_TOTAL);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createCODPayment_ShouldThrowException_WhenInValidOrderStatus() {
        // Arrange
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new InvalidOrderStatusException(INVALID_ORDER_STATUS))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Assert & Act
        assertThatThrownBy(() -> CODPaymentStrategy.createPayment(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining(INVALID_ORDER_STATUS);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void completeCODPayment_ShouldCompleteCODPayment_WhenValidRequest() throws MessagingException {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.of(paymentTransaction));
        // Act
        CODPaymentStrategy.completePayment(1L);
        // Assert
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        verify(paymentTransactionRepository).save(argThat(pt -> paymentTransaction.getPaymentStatus() == PaymentStatus.COMPLETED));
        verify(applicationEventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void completeCODPayment_ShouldThrowException_WhenOrderAlreadyCompleted() {
        // Arrange
        paymentTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.of(paymentTransaction));
        // Act & Assert
        assertThatThrownBy(() -> CODPaymentStrategy.completePayment(1L))
                .isInstanceOf(PaymentAlreadyCompletedException.class)
                .hasMessageContaining(ORDER_ALREADY_COMPLETED);
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void completeCODPayment_ShouldThrowException_WhenTransactionNotFound() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> CODPaymentStrategy.completePayment(1L))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(PAYMENT_NOT_FOUND);
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }
}