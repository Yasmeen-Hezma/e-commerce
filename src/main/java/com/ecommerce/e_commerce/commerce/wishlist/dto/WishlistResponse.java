package com.ecommerce.e_commerce.commerce.wishlist.dto;

import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class WishlistResponse {
    private Long id;
    private Long userId;
    private List<WishlistItemResponse> items;
    private List<StockWarning> warnings;
}
