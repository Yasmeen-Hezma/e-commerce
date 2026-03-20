package com.ecommerce.e_commerce.commerce.payment.validation;

import com.ecommerce.e_commerce.commerce.order.model.Order;
import com.ecommerce.e_commerce.common.exception.InvalidOrderTotalException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.ecommerce.e_commerce.common.utils.Constants.INVALID_ORDER_TOTAL;

@Component
public class OrderTotalValidator extends PaymentOrderValidator {
    @Override
    protected void doValidate(Order order) {
        if (order.getTotalPrice() == null || order.getTotalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOrderTotalException(INVALID_ORDER_TOTAL);
        }
    }
}
