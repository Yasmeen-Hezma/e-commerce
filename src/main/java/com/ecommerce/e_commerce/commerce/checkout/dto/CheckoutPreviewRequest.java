package com.ecommerce.e_commerce.commerce.checkout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class CheckoutPreviewRequest {
    private String governorate;
}
