package com.ecommerce.e_commerce.commerce.payment.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentOrderValidationChain {
    private final PaymentAlreadyCompletedValidator paymentAlreadyCompletedValidator;
    private final OrderTotalValidator orderTotalValidator;
    private final OrderStatusValidator orderStatusValidator;

    // build the order of the validations, and return the head (first validator)
    public PaymentOrderValidator build() {
        paymentAlreadyCompletedValidator
                .linkWith(orderTotalValidator)
                .linkWith(orderStatusValidator);
        return paymentAlreadyCompletedValidator;
    }
}
