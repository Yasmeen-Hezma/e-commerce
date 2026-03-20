package com.ecommerce.e_commerce.commerce.cart.mapper;

import com.ecommerce.e_commerce.commerce.cart.dto.CartResponse;
import com.ecommerce.e_commerce.commerce.cart.model.Cart;
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        uses = {CartItemMapper.class}
)
public interface CartMapper {
    @Mapping(target = "userId", source = "user.userId")
    @Mapping(target = "items", source = "cartItems")
    @Mapping(target = "id", source = "cartId")
    CartResponse toResponse(Cart cart);

    default CartResponse toResponseWithWarnings(Cart cart, List<StockWarning> warnings) {
        CartResponse response = toResponse(cart);
        response.setWarnings(warnings);
        return response;
    }
}
