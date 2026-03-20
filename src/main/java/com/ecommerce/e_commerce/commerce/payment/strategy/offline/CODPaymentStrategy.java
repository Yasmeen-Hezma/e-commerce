package com.ecommerce.e_commerce.commerce.payment.strategy.offline;

import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.event.OrderCompletedEvent;
import com.ecommerce.e_commerce.commerce.order.mapper.OrderMapper;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentMethod;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentStatus;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.commerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidationChain;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.common.exception.PaymentAlreadyCompletedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import static com.ecommerce.e_commerce.common.utils.Constants.ORDER_ALREADY_COMPLETED;
import static com.ecommerce.e_commerce.common.utils.Constants.PAYMENT_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class CODPaymentStrategy implements OfflinePaymentStrategy {
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final PaymentOrderValidationChain paymentOrderValidationChain;
    private final ApplicationEventPublisher applicationEventPublisher;
    public static final String USD = "USD";

    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.COD;
    }

    @Override
    public OrderResponse createPayment(Long orderId) {
        Order order = orderService.getOrderById(orderId);
        paymentOrderValidationChain.build().validate(order);

        PaymentTransaction paymentTransaction = createPaymentTransactionCOD(order);
        paymentTransactionRepository.save(paymentTransaction);

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public void completePayment(Long orderId) {
        Order order = orderService.getOrderById(orderId);
        PaymentTransaction paymentTransaction = getPaymentTransactionByOrder(order);

        if (PaymentStatus.COMPLETED.equals(paymentTransaction.getPaymentStatus())) {
            throw new PaymentAlreadyCompletedException(ORDER_ALREADY_COMPLETED);
        }

        paymentTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
        paymentTransactionRepository.save(paymentTransaction);
        applicationEventPublisher
                .publishEvent(new OrderCompletedEvent(orderId, order.getTotalPrice(), order.getUser().getAuthUser().getEmail()));
    }

    private PaymentTransaction createPaymentTransactionCOD(Order order) {
        return PaymentTransaction
                .builder()
                .order(order)
                .paymentMethod(PaymentMethod.COD)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(order.getTotalPrice())
                .currency(USD)
                .build();
    }

    private PaymentTransaction getPaymentTransactionByOrder(Order order) {
        return paymentTransactionRepository.findByOrder(order)
                .orElseThrow(() -> new ItemNotFoundException(PAYMENT_NOT_FOUND));
    }
}
