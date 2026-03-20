package com.ecommerce.e_commerce.commerce.payment.strategy.online;

import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.order.event.OrderCompletedEvent;
import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.commerce.order.repository.OrderRepository;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlineCaptureResponse;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlinePaymentResponse;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentMethod;
import com.ecommerce.e_commerce.commerce.payment.enums.PaymentStatus;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.commerce.payment.repository.PaymentTransactionRepository;
import com.ecommerce.e_commerce.commerce.payment.validation.PaymentOrderValidationChain;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.common.exception.PayPalApprovalUrlNotFoundException;
import com.ecommerce.e_commerce.common.exception.PayPalCaptureException;
import com.ecommerce.e_commerce.common.exception.PayPalOrderMismatchException;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.ecommerce.e_commerce.common.utils.Constants.*;
import static com.ecommerce.e_commerce.common.utils.Constants.PAYPAL_ORDER_ID_MISMATCH;

@Component
@RequiredArgsConstructor
public class PaypalPaymentStrategy implements OnlinePaymentStrategy {
    @Override
    public PaymentMethod getPaymentMethod() {
        return PaymentMethod.PAYPAL;
    }

    public static final String APPROVE = "approve";
    public static final String USD = "USD";
    public static final String LIVE = "live";
    public static final String COMPLETED = "COMPLETED";
    public static final String E_COMMERCE = "E-COMMERCE";
    public static final String BILLING = "BILLING";
    public static final String PAY_NOW = "PAY_NOW";
    public static final String CAPTURE = "CAPTURE";
    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final OrderService orderService;
    private final PaymentOrderValidationChain paymentOrderValidationChain;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${paypal.client-id}")
    private String paypalClientId;
    @Value("${paypal.client-secret}")
    private String paypalClientSecret;
    @Value(("${paypal.mode}"))
    private String paypalMode;
    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public OnlinePaymentResponse createPayment(Long orderId) {
        // 1) Get and validate order
        Order order = orderService.getOrderById(orderId);
        paymentOrderValidationChain.build().validate(order);
        // 2) Create PayPal client
        PayPalHttpClient client = createPaypalClient();
        // 3) Build PayPal order request
        OrderRequest orderRequest = buildPayPalOrderRequest(order);
        OrdersCreateRequest request = new OrdersCreateRequest();
        request.prefer("return=representation");
        request.requestBody(orderRequest);
        try {
            // 4) Execute PayPal API call
            HttpResponse<com.paypal.orders.Order> response = client.execute(request);
            com.paypal.orders.Order paypalOrder = response.result();
            // 5) Save payment transaction
            PaymentTransaction paymentTransaction = createPaymentTransactionPayPal(order, paypalOrder);
            paymentTransactionRepository.save(paymentTransaction);
            // 6) Update order status
            order.setStatus(OrderStatus.PENDING_PAYMENT);
            return buildPayPalOrderResponse(paypalOrder, order);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public OnlineCaptureResponse capturePayment(Long orderId, String paypalOrderId) {
        // 1) Get order and payment transaction
        Order order = orderService.getOrderById(orderId);
        PaymentTransaction paymentTransaction = getPaymentTransactionByOrder(order);
        // 2) Verify Paypal Order ID matches
        validatePayPalOrderId(paypalOrderId, paymentTransaction);
        // 3) Create PayPal client and capture request
        PayPalHttpClient client = createPaypalClient();
        OrdersCaptureRequest request = new OrdersCaptureRequest(paypalOrderId);
        request.prefer("return=representation");
        try {
            // 4) Execute capture
            HttpResponse<com.paypal.orders.Order> response = client.execute(request);
            com.paypal.orders.Order capturedOrder = response.result();
            // 5) Verify capture was successful
            if (COMPLETED.equals(capturedOrder.status())) {
                String captureId = extractCaptureId(capturedOrder);

                paymentTransaction.setPaymentStatus(PaymentStatus.COMPLETED);
                paymentTransaction.setExternalTransactionId(captureId);
                paymentTransactionRepository.save(paymentTransaction);

                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                applicationEventPublisher
                        .publishEvent(new OrderCompletedEvent(orderId, order.getTotalPrice(), order.getUser().getAuthUser().getEmail()));
                return OnlineCaptureResponse
                        .builder()
                        .orderId(orderId)
                        .externalPaymentId(paypalOrderId)
                        .captureId(captureId)
                        .status(COMPLETED)
                        .amount(paymentTransaction.getAmount())
                        .build();
            } else {
                throw new PayPalCaptureException(PAYPAL_CAPTURE_WAS_NOT_COMPLETED);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OnlinePaymentResponse buildPayPalOrderResponse(com.paypal.orders.Order paypalOrder, Order order) {
        String approvalUrl = paypalOrder
                .links()
                .stream()
                .filter(link -> APPROVE.equals(link.rel()))
                .findFirst()
                .map(LinkDescription::href)
                .orElseThrow(() -> new PayPalApprovalUrlNotFoundException(NO_APPROVAL_URL_FOUND_IN_PAYPAL_RESPONSE));
        return OnlinePaymentResponse
                .builder()
                .externalPaymentId(paypalOrder.id())
                .orderId(order.getOrderId())
                .status(paypalOrder.status())
                .approvalUrl(approvalUrl)
                .amount(order.getTotalPrice())
                .currency(USD)
                .build();
    }

    private PaymentTransaction createPaymentTransactionPayPal(Order order, com.paypal.orders.Order paypalOrder) {
        return PaymentTransaction
                .builder()
                .order(order)
                .paymentMethod(PaymentMethod.PAYPAL)
                .paymentStatus(PaymentStatus.PENDING)
                .paypalOrderId(paypalOrder.id())
                .amount(order.getTotalPrice())
                .currency(USD)
                .build();
    }

    private OrderRequest buildPayPalOrderRequest(Order order) {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent(CAPTURE);

        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(baseUrl + "api/v1/order/" + order.getOrderId() + "/payment/paypal/success")
                .cancelUrl(baseUrl + "api/v1/order/" + order.getOrderId() + "/payment/paypal/cancel")
                .brandName(E_COMMERCE)
                .landingPage(BILLING)
                .userAction(PAY_NOW);
        orderRequest.applicationContext(applicationContext);

        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();
        purchaseUnits.add(new PurchaseUnitRequest()
                .referenceId(order.getOrderId().toString())
                .description("Order #" + order.getOrderId())
                .customId(order.getOrderId().toString())
                .softDescriptor(E_COMMERCE)
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode(USD)
                        .value(order.getTotalPrice().toString())));
        orderRequest.purchaseUnits(purchaseUnits);
        return orderRequest;
    }

    private PayPalHttpClient createPaypalClient() {
        PayPalEnvironment environment;
        if (LIVE.equalsIgnoreCase(paypalMode)) {
            environment = new PayPalEnvironment.Live(paypalClientId, paypalClientSecret);
        } else {
            environment = new PayPalEnvironment.Sandbox(paypalClientId, paypalClientSecret);
        }
        return new PayPalHttpClient(environment);
    }

    private PaymentTransaction getPaymentTransactionByOrder(Order order) {
        return paymentTransactionRepository.findByOrder(order)
                .orElseThrow(() -> new ItemNotFoundException(PAYMENT_NOT_FOUND));
    }

    private void validatePayPalOrderId(String paypalOrderId, PaymentTransaction paymentTransaction) {
        if (!paypalOrderId.equals(paymentTransaction.getPaypalOrderId())) {
            throw new PayPalOrderMismatchException(PAYPAL_ORDER_ID_MISMATCH);
        }
    }

    private String extractCaptureId(com.paypal.orders.Order capturedOrder) {
        return capturedOrder.purchaseUnits().getFirst().payments().captures().getFirst().id();
    }
}
