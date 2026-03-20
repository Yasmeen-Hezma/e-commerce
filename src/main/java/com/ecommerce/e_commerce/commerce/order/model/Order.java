package com.ecommerce.e_commerce.commerce.order.model;

import com.ecommerce.e_commerce.commerce.order.enums.OrderStatus;
import com.ecommerce.e_commerce.commerce.payment.model.PaymentTransaction;
import com.ecommerce.e_commerce.user.profile.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Entity
@Table(name = "orders", schema = "e-commerce")
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private PaymentTransaction paymentTransaction;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "subtotal", precision = 19, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "product_discounts", precision = 19, scale = 2)
    private BigDecimal productDiscounts;

    @Column(name = "first_order_discount", precision = 19, scale = 2)
    private BigDecimal firstOrderDiscount;

    @Column(name = "promo_code_used", length = 50)
    private String promoCodeUsed;                    // ← The code itself

    @Column(name = "promo_discount", precision = 19, scale = 2)
    private BigDecimal promoDiscount;                // ← The amount saved

    @Column(name = "shipping", precision = 19, scale = 2)
    private BigDecimal shipping;

    @Column(name = "tax", precision = 19, scale = 2)
    private BigDecimal tax;

    @Column(name = "total_price", precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "shipping_governorate")
    private String shippingGovernorate;

    @Column(name = "shipping_city")
    private String shippingCity;

    @Column(name = "shipping_street")
    private String shippingStreet;

    @Column(name = "shipping_floor_number")
    private String shippingFloorNumber;

    @Column(name = "shipping_apartment_number")
    private String shippingApartmentNumber;

    @Column(name = "shipping_phone")
    private String shippingPhone;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    @Column(name = "customer_first_name")
    private String customerFirstName;

    @Column(name = "customer_last_name")
    private String customerLastName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public boolean hasShippingAddress() {
        return shippingGovernorate != null &&
                shippingCity != null &&
                shippingStreet != null &&
                shippingPhone != null;
    }
}
