package com.ecommerce.e_commerce.commerce.payment.strategy.online;

import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentMethod;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentStatus;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.commerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidationChain;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidator;
import com.ecommerce.e_commerce.common.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static com.ecommerce.e_commerce.common.utils.Constants.*;
import static com.ecommerce.e_commerce.common.utils.Constants.PAYMENT_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaypalPaymentStrategyTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentOrderValidationChain paymentOrderValidationChain;
    @Mock
    private PaymentOrderValidator paymentOrderValidator;
    @InjectMocks
    private PaypalPaymentStrategy paypalPaymentStrategy;

    private Order order;
    private PaymentTransaction paymentTransaction;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paypalPaymentStrategy, "paypalClientId", "test-client-id");
        ReflectionTestUtils.setField(paypalPaymentStrategy, "paypalClientSecret", "test-client-secret");
        ReflectionTestUtils.setField(paypalPaymentStrategy, "paypalMode", "sandbox");
        ReflectionTestUtils.setField(paypalPaymentStrategy, "baseUrl", "http://localhost:8080/");

        order = Order
                .builder()
                .orderId(1L)
                .totalPrice(BigDecimal.valueOf(100.00))
                .status(OrderStatus.PENDING)
                .build();
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
    }

    @Test
    void createPaypalPayment_ShouldThrowException_WhenOrderAlreadyCompleted() {
        // Arrange
        paymentTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
        order.setPaymentTransaction(paymentTransaction);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new PaymentAlreadyCompletedException(ORDER_ALREADY_COMPLETED))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Act & Assert
        assertThatThrownBy(() -> paypalPaymentStrategy.createPayment(1L))
                .isInstanceOf(PaymentAlreadyCompletedException.class)
                .hasMessageContaining(ORDER_ALREADY_COMPLETED);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void createPaypalPayment_ShouldThrowException_WhenInvalidOrderTotal() {
        // Arrange
        order.setTotalPrice(BigDecimal.ZERO);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new InvalidOrderTotalException(INVALID_ORDER_TOTAL))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Act & Assert
        assertThatThrownBy(() -> paypalPaymentStrategy.createPayment(1L))
                .isInstanceOf(InvalidOrderTotalException.class)
                .hasMessageContaining(INVALID_ORDER_TOTAL);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void createPaypalPayment_ShouldThrowException_WhenInvalidOrderStatus() {
        // Arrange
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentOrderValidationChain.build()).thenReturn(paymentOrderValidator);
        doThrow(new InvalidOrderStatusException(INVALID_ORDER_STATUS))
                .when(paymentOrderValidator)
                .validate(any(Order.class));
        // Act & Assert
        assertThatThrownBy(() -> paypalPaymentStrategy.createPayment(1L))
                .isInstanceOf(InvalidOrderStatusException.class)
                .hasMessageContaining(INVALID_ORDER_STATUS);
        verify(orderService).getOrderById(1L);
        verify(paymentOrderValidationChain).build();
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    void capturePayPalPayment_ShouldThrowException_WhenPayPalOrderMisMatch() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.of(paymentTransaction));
        // Act & Assert
        assertThatThrownBy(() -> paypalPaymentStrategy.capturePayment(1L, "WRONG-ORDER-ID"))
                .isInstanceOf(PayPalOrderMismatchException.class)
                .hasMessageContaining(PAYPAL_ORDER_ID_MISMATCH);
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void capturePayPalPayment_ShouldThrowException_WhenPaymentNotFound() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.empty());
        // Act & Assert
        assertThatThrownBy(() -> paypalPaymentStrategy.capturePayment(1L, "PAYPAL-ORDER-123"))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(PAYMENT_NOT_FOUND);
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(orderRepository, never()).save(any(Order.class));
    }
}