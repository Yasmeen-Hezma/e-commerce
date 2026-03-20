package com.ecommerce.e_commerce.commerce.order.controller;

import com.ecommerce.e_commerce.commerce.order.dto.OrderResponse;
import com.ecommerce.e_commerce.commerce.order.dto.ShippingAddressRequest;
import com.ecommerce.e_commerce.commerce.order.service.OrderService;
import com.ecommerce.e_commerce.commerce.payment.dto.PaymentStatusResponse;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlineCaptureResponse;
import com.ecommerce.e_commerce.commerce.payment.dto.OnlinePaymentResponse;
import com.ecommerce.e_commerce.commerce.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/order/")
@Tag(name = "Order", description = "Order Management APIs")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final PaymentService paymentService;

    @Operation(summary = "Create new order")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(HttpServletRequest request
            , @RequestParam(required = false) String promoCode
            , @RequestParam String governorate) {
        return ResponseEntity.ok(orderService.createOrderFromCart(request, promoCode, governorate));
    }

    @Operation(summary = "Add shipping address to an order")
    @PatchMapping("{orderId}/shipping-address")
    public ResponseEntity<OrderResponse> addShippingAddress(
            @PathVariable Long orderId,
            @Valid @RequestBody ShippingAddressRequest addressRequest,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(orderService.addShippingAddress(orderId, addressRequest, request));
    }

    @Operation(summary = "Create PayPal payment")
    @PostMapping("{orderId}/payment/paypal/create")
    public ResponseEntity<OnlinePaymentResponse> createPayPalPayment(@PathVariable Long orderId) {
        OnlinePaymentResponse response = paymentService.createPaypalPayment(orderId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Capture PayPal payment")
    @PostMapping("{orderId}/payment/paypal/capture")
    public ResponseEntity<OnlineCaptureResponse> capturePayPalPayment(@PathVariable Long orderId, @RequestParam String token) {
        OnlineCaptureResponse response = paymentService.capturePayPalPayment(orderId, token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Handle PayPal payment success")
    @GetMapping("{orderId}/payment/paypal/success")
    public ResponseEntity<String> handlePayPalSuccess(@PathVariable Long orderId, @RequestParam String token) {
        return ResponseEntity.ok()
                .header("Location", "/payment/success?orderId=" + orderId + "&token=" + token)
                .body("Redirecting to payment completion...");

    }

    @Operation(summary = "Handle PayPal payment cancel")
    @GetMapping("{orderId}/payment/paypal/cancel")
    public ResponseEntity<String> handlePayPalCancel(@PathVariable Long orderId, @RequestParam String token) {
        paymentService.handlePaymentFailure(orderId);
        return ResponseEntity.ok()
                .header("Location", "/payment/cancelled?orderId=" + orderId)
                .body("Payment was cancelled");
    }

    @Operation(summary = "Get order by id")
    @GetMapping("{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderResponseById(orderId));
    }

    @Operation(summary = "Get status of payment by order-id")
    @GetMapping("{orderId}/payment/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(orderId));
    }

    @Operation(summary = "Confirm Cash On Delivery payment")
    @PostMapping("{orderId}/payment/cod/confirm")
    public ResponseEntity<OrderResponse> confirmCODPayment(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.createCODPayment(orderId));
    }

    @Operation(summary = "Complete Cash On Delivery payment")
    @PatchMapping("{orderId}/payment/cod/complete")
    public ResponseEntity<Void> completeCODPayment(@PathVariable Long orderId) {
        paymentService.completeCODPayment(orderId);
        return ResponseEntity.noContent().build();
    }

}
