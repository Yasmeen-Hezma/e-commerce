package com.ecommerce.e_commerce.commerce.wishlist.mapper;

import com.ecommerce.e_commerce.common.utils.MapperUtil;
import com.ecommerce.e_commerce.commerce.wishlist.dto.WishlistItemResponse;
import com.ecommerce.e_commerce.commerce.wishlist.model.WishlistItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {MapperUtil.class},
        // it tells MapStruct to skip setting target with (null) if the source is (null)
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface WishlistItemMapper {
    @Mapping(source = "product.productName", target = "productName")
    @Mapping(source = "product.productId", target = "productId")
    @Mapping(source = "product.image", target = "image", qualifiedByName = "mapImageUrl")
    @Mapping(source = "product.quantity", target = "maxQuantity")
    @Mapping(source = "product.price", target = "originalPrice")
    @Mapping(source = "product.discount", target = "discountPercent")
    @Mapping(source = "priceSnapshot", target = "priceSnapshot")
    @Mapping(target = "lineTotal",
            expression = "java(wishlistItem.getPriceSnapshot().multiply(BigDecimal.valueOf(wishlistItem.getQuantity())))")
    WishlistItemResponse toResponse(WishlistItem wishlistItem);
}
