package com.ecommerce.e_commerce.commerce.payment.service;

import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlineCaptureResponse;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlinePaymentResponse;
import com.ecommerce.e_commerce.commerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentMethod;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentStatus;
import com.ecommerce.e_commerce.commerce.payment.factory.PaymentStrategyFactory;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.commerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.e_commerce.commerce.payment.strategy.offline.OfflinePaymentStrategy;
import com.ecommerce.e_commerce.commerce.payment.strategy.online.OnlinePaymentStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;
    @Mock
    private OnlinePaymentStrategy onlinePaymentStrategy;
    @Mock
    private OfflinePaymentStrategy offlinePaymentStrategy;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order order;
    private PaymentTransaction paymentTransaction;

    @BeforeEach
    void setUp() {
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
    void createPayPalPayment_ShouldDelegateToOnlineStrategy_WhenValidRequest() {
        // Arrange
        OnlinePaymentResponse expectedResponse = OnlinePaymentResponse
                .builder()
                .orderId(1L)
                .externalPaymentId("PAYPAL-ORDER-123")
                .status("CREATED")
                .approvalUrl("https://sandbox.paypal.com/approve/PAYPAL-ORDER-123")
                .amount(BigDecimal.valueOf(100.00))
                .currency("USD")
                .build();
        when(paymentStrategyFactory.getOnlineStrategy(PaymentMethod.PAYPAL)).thenReturn(onlinePaymentStrategy);
        when(onlinePaymentStrategy.createPayment(1L)).thenReturn(expectedResponse);
        // Act
        OnlinePaymentResponse result = paymentService.createPaypalPayment(1L);
        // Assert
        verify(paymentStrategyFactory).getOnlineStrategy(PaymentMethod.PAYPAL);
        verify(onlinePaymentStrategy).createPayment(1L);
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void capturePayPalPayment_ShouldDelegateToOnlineStrategy_WhenValidRequest() {
        // Arrange
        OnlineCaptureResponse expectedResponse = OnlineCaptureResponse
                .builder()
                .orderId(1L)
                .externalPaymentId("PAYPAL-ORDER-123")
                .captureId("CAPTURE-456")
                .status("COMPLETED")
                .amount(BigDecimal.valueOf(100.00))
                .build();
        when(paymentStrategyFactory.getOnlineStrategy(PaymentMethod.PAYPAL)).thenReturn(onlinePaymentStrategy);
        when(onlinePaymentStrategy.capturePayment(1L, "PAYPAL-ORDER-123")).thenReturn(expectedResponse);
        // Act
        OnlineCaptureResponse result = paymentService.capturePayPalPayment(1L, "PAYPAL-ORDER-123");
        // Assert
        verify(paymentStrategyFactory).getOnlineStrategy(PaymentMethod.PAYPAL);
        verify(onlinePaymentStrategy).capturePayment(1L, "PAYPAL-ORDER-123");
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void createCODPayment_ShouldDelegateToOfflineStrategy_WhenValidRequest() {
        // Arrange
        OrderResponse expectedResponse = OrderResponse
                .builder()
                .id(1L)
                .build();
        when(paymentStrategyFactory.getOfflineStrategy(PaymentMethod.COD)).thenReturn(offlinePaymentStrategy);
        when(offlinePaymentStrategy.createPayment(1L)).thenReturn(expectedResponse);
        // Act
        OrderResponse result = paymentService.createCODPayment(1L);
        // Assert
        verify(paymentStrategyFactory).getOfflineStrategy(PaymentMethod.COD);
        verify(offlinePaymentStrategy).createPayment(1L);
        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void completeCODPayment_ShouldDelegateToOfflineStrategy_WhenValidRequest() {
        // Arrange
        when(paymentStrategyFactory.getOfflineStrategy(PaymentMethod.COD)).thenReturn(offlinePaymentStrategy);
        // Act
        paymentService.completeCODPayment(1L);
        // Assert
        verify(paymentStrategyFactory).getOfflineStrategy(PaymentMethod.COD);
        verify(offlinePaymentStrategy).completePayment(1L);
    }

    @Test
    void handlePaymentFailure_ShouldUpdatePaymentStatus_WhenPaymentExists() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.of(paymentTransaction));
        // Act
        paymentService.handlePaymentFailure(1L);
        // Assert
        verify(paymentTransactionRepository).save(argThat(pt -> pt.getPaymentStatus() == PaymentStatus.CANCELED));
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_FAILED));
    }

    @Test
    void handlePaymentFailure_ShouldUpdateOrderStatusOnly_WhenPaymentNotExist() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.empty());
        // Act
        paymentService.handlePaymentFailure(1L);
        // Assert
        verify(paymentTransactionRepository, never()).save(any(PaymentTransaction.class));
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAYMENT_FAILED));
    }

    @Test
    void getPaymentStatus_ShouldReturnCompleteStatus_WhenTransactionFound() {
        // Arrange
        paymentTransaction.setCreatedAt(Instant.now());
        paymentTransaction.setUpdatedAt(Instant.now());
        paymentTransaction.setExternalTransactionId("CAPTURE-123");
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.of(paymentTransaction));
        // Act
        PaymentStatusResponse result = paymentService.getPaymentStatus(1L);
        // Assert
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getPaymentMethod()).isEqualTo(PaymentMethod.PAYPAL);
        assertThat(result.getCaptureId()).isEqualTo("CAPTURE-123");
        assertThat(result.getPaypalOrderId()).isEqualTo("PAYPAL-ORDER-123");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getAmount()).isEqualTo(BigDecimal.valueOf(100.00));
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
    }

    @Test
    void getPaymentStatus_ShouldReturnBasicStatus_WhenTransactionNotFound() {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(order);
        when(paymentTransactionRepository.findByOrder(order)).thenReturn(Optional.empty());
        // Act
        PaymentStatusResponse result = paymentService.getPaymentStatus(1L);
        // Assert
        verify(orderService).getOrderById(1L);
        verify(paymentTransactionRepository).findByOrder(order);
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING.toString());
        assertThat(result.getPaymentStatus()).isNull();
        assertThat(result.getPaymentMethod()).isNull();
    }
}