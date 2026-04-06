package com.ecommerce.e_commerce.commerce.payment.validation;

import com.ecommerce.e_commerce.commerce.order.model.Order;

public abstract class PaymentOrderValidator {
    // Reference to the next validator in the chain
    private PaymentOrderValidator next;

    public PaymentOrderValidator linkWith(PaymentOrderValidator next) {
        this.next = next;
        return next;
    }

    // Executes current validation, then delegates to the next validator if no exception occurs
    public void validate(Order order) {
        doValidate(order);
        if (next != null) {
            next.validate(order);
        }
    }

    // Each concrete validator implements its own validation logic here
    protected abstract void doValidate(Order order);
}
