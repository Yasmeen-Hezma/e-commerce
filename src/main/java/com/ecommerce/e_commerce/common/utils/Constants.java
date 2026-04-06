package com.ecommerce.e_commerce.common.utils;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class Constants {

    // Authentication & Authorization
    public static final String REFRESH_TOKEN_IS_INVALID_OR_EXPIRED = "The refresh token is invalid or expired.";
    public static final String EMAIL_IS_NOT_VERIFIED = "Email is not verified";
    public static final String YOU_CAN_ONLY_ACCESS_YOUR_OWN_ORDERS = "You can only access your own orders";
    public static final String YOU_CAN_ONLY_ACCESS_YOUR_OWN_REVIEWS = "You can only access your own reviews";
    public static final String ROLE_NOT_FOUND = "Role not found";

    // Resource Not Found
    public static final String CATEGORY_NOT_FOUND = "Category not found";
    public static final String BRAND_NOT_FOUND = "Brand not found";
    public static final String PRODUCT_NOT_FOUND = "Product not found";
    public static final String PRODUCT_STATUS_NOT_FOUND = "Product status not found";
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String PAYMENT_NOT_FOUND = "Payment not found";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String EMAIL_NOT_FOUND = "Email not found";
    public static final String NO_USER_FOUND_WITH_THIS_EMAIL = "No user found with this email";
    public static final String DEFAULT_SHIPPING_ADDRESS_NOT_FOUND_FOR_USER = "Default shipping address not found for user";
    public static final String REVIEW_NOT_FOUND = "Review not found";
    public static final String REVIEW_NOT_FOUND_FOR_THIS_PRODUCT = "Review not found for this product";

    // Already Exists / Conflict
    public static final String EMAIL_ALREADY_EXISTS = "Email already exists";
    public static final String PRODUCT_ALREADY_EXISTS = "Product already exists";
    public static final String BRAND_ALREADY_EXISTS = "Brand already exists";
    public static final String CATEGORY_ALREADY_EXISTS = "Category already exists";
    public static final String YOU_HAVE_ALREADY_REVIEWED_THIS_PRODUCT = "You have already reviewed this product";

    // Orders & Checkout
    public static final String ORDER_ALREADY_COMPLETED = "Order already has a completed payment";
    public static final String INVALID_ORDER_TOTAL = "Invalid order total for payment";
    public static final String INVALID_ORDER_STATUS = "Invalid order status";
    public static final String CANNOT_MODIFY_SHIPPING_ADDRESS_FOR_THIS_ORDER = "Cannot modify shipping address for this order";
    public static final String ORDER_ALREADY_HAS_A_SHIPPING_ADDRESS = "Order already has a shipping address";
    public static final String STOCK_ISSUES = "Stock issues detected for some products";
    public static final String CART_IS_EMPTY = "Cart is empty";
    public static final String FAILED_TO_SEND_ORDER_CONFIRMATION_EMAIL = "Failed to send order confirmation email";

    // Shipping Address Validation
    public static final String GOVERNORATE_IS_REQUIRED = "Governorate is required";
    public static final String CITY_IS_REQUIRED = "City is required";
    public static final String STREET_IS_REQUIRED = "Street is required";
    public static final String PHONE_IS_REQUIRED = "Phone is required";

    // PayPal & Payment Processing
    public static final String PAYPAL_ORDER_ID_MISMATCH = "PayPal order ID mismatch";
    public static final String NO_APPROVAL_URL_FOUND_IN_PAYPAL_RESPONSE = "No approval URL found in PayPal response";
    public static final String PAYPAL_CAPTURE_WAS_NOT_COMPLETED = "PayPal capture was not completed";
    public static final String PAYMENT_SUPPORT_IS_MISSING_FOR_THIS_METHOD = "Payment support is missing for this method";

    // OTP / Password Reset
    public static final String OTP_EXPIRED = "OTP expired";
    public static final String INVALID_OTP = "Invalid OTP";
    public static final String OTP_NOT_FOUND_FOR_THIS_EMAIL = "Otp not found for this email";
    public static final String IF_AN_ACCOUNT_EXISTS_A_PASSWORD_RESET_OTP_HAS_BEEN_SENT = "If an account exists, a password reset OTP has been sent";
    public static final String PASSWORD_HAS_BEEN_RESET_SUCCESSFULLY = "Password has been reset successfully";
    public static final String CURRENT_PASSWORD_IS_INCORRECT = "Current password is incorrect";
    public static final String NEW_PASSWORD_MUST_BE_DIFFERENT_FROM_CURRENT_PASSWORD = "New password must be different from current password";

    // File Handling
    public static final String FAILED_TO_SAVE_IMAGE = "Failed to save image";

    // Pricing
    public static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("500.00");
    public static final BigDecimal CAIRO_SHIPPING = new BigDecimal("30.00");
    public static final BigDecimal ALEXANDRIA_SHIPPING = new BigDecimal("40.00");
    public static final BigDecimal DEFAULT_SHIPPING = new BigDecimal("40.00");
    public static final BigDecimal SAVE_15_PROMO_CODE = new BigDecimal("0.15");
    public static final BigDecimal SAVE_20_PROMO_CODE = new BigDecimal("0.20");
    public static final BigDecimal FIRST_ORDER_DISCOUNT = new BigDecimal("50.00");
    public static final String INVALID_PROMO_CODE = "Invalid promo code";
    public static final String SAVE_15 = "SAVE15";
    public static final String SAVE_20 = "SAVE20";
    public static final String CAIRO = "cairo";
    public static final String GIZA = "giza";
    public static final String ALEXANDRIA = "alexandria";

    // General
    public static final String ERROR_HANDLING_REQUEST = "Error handling request";
}
