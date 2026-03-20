package com.ecommerce.e_commerce.commerce.wishlist.service;

import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.repository.UserRepository;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import com.ecommerce.e_commerce.commerce.wishlist.dto.UpdateWishlistItemRequest;
import com.ecommerce.e_commerce.commerce.wishlist.dto.WishlistItemRequest;
import com.ecommerce.e_commerce.commerce.wishlist.dto.WishlistItemResponse;
import com.ecommerce.e_commerce.commerce.wishlist.dto.WishlistResponse;
import com.ecommerce.e_commerce.commerce.wishlist.mapper.WishlistItemMapper;
import com.ecommerce.e_commerce.commerce.wishlist.mapper.WishlistMapper;
import com.ecommerce.e_commerce.commerce.wishlist.model.Wishlist;
import com.ecommerce.e_commerce.commerce.wishlist.model.WishlistItem;
import com.ecommerce.e_commerce.commerce.wishlist.model.WishlistItemId;
import com.ecommerce.e_commerce.commerce.wishlist.repository.WishlistItemRepository;
import com.ecommerce.e_commerce.commerce.wishlist.repository.WishlistRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.ecommerce.e_commerce.common.utils.Constants.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductService productService;
    private final WishlistMapper wishlistMapper;
    private final WishlistItemMapper wishlistItemMapper;
    private final PricingService pricingService;

    private record WishlistItemResult(WishlistItem wishlistItem, boolean isNew) {

    }

    @Override
    @Transactional
    public WishlistItemResponse addItemToWishlist(HttpServletRequest request, WishlistItemRequest wishlistItemRequest) {
        long userId = userService.getUserId(request);
        Wishlist wishlist = getOrCreateWishlist(userId);
        WishlistItemResult wishlistItemResult = findOrCreateWishListItem(wishlist, wishlistItemRequest);
        updateWishlistItemQuantity(wishlistItemResult, wishlistItemRequest.getQuantity());
        saveWishlistAndItem(wishlist, wishlistItemResult.wishlistItem);
        return wishlistItemMapper.toResponse(wishlistItemResult.wishlistItem);
    }

    @Override
    public WishlistResponse getWishlistByUser(HttpServletRequest request) {
        Wishlist wishlist = getAuthenticatedUserWishlist(request);
        return wishlistMapper.toResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse syncWishlistSnapshot(HttpServletRequest request, UpdateWishlistItemRequest updateRequest) {
        Wishlist wishlist = getAuthenticatedUserWishlist(request);
        List<StockWarning> warnings = new ArrayList<>();
        List<WishlistItem> updatedItems = buildMergedWishlistItems(updateRequest, wishlist, warnings);
        updateWishlist(updatedItems, wishlist);
        return wishlistMapper.toResponseWithWarnings(wishlist, warnings);
    }

    @Transactional
    @Override
    public void clearWishlist(HttpServletRequest request) {
        Wishlist wishlist = getAuthenticatedUserWishlist(request);
        wishlist.clearItems();
    }

    private Wishlist getOrCreateWishlist(long userId) {
        return wishlistRepository
                .findByUser_UserId(userId)
                .orElseGet(() -> createAndSaveNewWishlist(userId));
    }

    private void updateWishlist(List<WishlistItem> updateItems, Wishlist wishlist) {
        wishlist.getWishlistItems().clear();
        wishlist.setWishlistItems(updateItems);
    }

    private List<WishlistItem> buildMergedWishlistItems(UpdateWishlistItemRequest updateRequest, Wishlist wishlist, List<StockWarning> warnings) {
        Map<Long, WishlistItem> existingItemsMap = buildExistingItemsMap(wishlist);
        removeUnwantedItems(updateRequest, existingItemsMap);
        mergeWishlistItems(updateRequest, wishlist, existingItemsMap, warnings);
        return new ArrayList<>(existingItemsMap.values());
    }

    private void removeUnwantedItems(UpdateWishlistItemRequest updateRequest, Map<Long, WishlistItem> existingItemsMap) {
        Set<Long> requestedIds = updateRequest
                .getWishlistItems()
                .stream()
                .map(WishlistItemRequest::getProductId)
                .collect(Collectors.toSet());
        existingItemsMap.entrySet().removeIf(entry -> !requestedIds.contains(entry.getKey()));
    }

    private void mergeWishlistItems(UpdateWishlistItemRequest updateRequest, Wishlist wishlist, Map<Long, WishlistItem> existingItemsMap, List<StockWarning> warnings) {
        for (WishlistItemRequest item : updateRequest.getWishlistItems()) {
            Long productId = item.getProductId();
            int quantity = item.getQuantity();
            Product product = productService.getNonDeletedProductById(productId);
            Optional<StockWarning> warningOpt = productService.checkStockAndWarn(product, quantity);
            if (warningOpt.isPresent()) {
                warnings.add(warningOpt.get());
                continue;
            }
            if (existingItemsMap.containsKey(productId)) {
                existingItemsMap.get(productId).setQuantity(quantity);
            } else {
                WishlistItem newItem = buildWishlistItem(wishlist, product, quantity);
                existingItemsMap.put(productId, newItem);
            }
        }
    }

    private WishlistItem buildWishlistItem(Wishlist wishlist, Product product, int quantity) {
        WishlistItemId id = setWishlistItemId(wishlist.getWishlistId(), product.getProductId());
        return WishlistItem.builder()
                .id(id)
                .product(product)
                .wishlist(wishlist)
                .quantity(quantity)
                .priceSnapshot(pricingService.calculatePriceSnapshot(product))
                .build();
    }

    private WishlistItem createWishListItem(Wishlist wishlist, WishlistItemRequest request) {
        Product product = productService.getNonDeletedProductById(request.getProductId());
        return buildWishlistItem(wishlist, product, request.getQuantity());
    }

    private WishlistItemResult findOrCreateWishListItem(Wishlist wishlist, WishlistItemRequest request) {
        return wishlist
                .getWishlistItems()
                .stream()
                .filter(item -> item.getProduct().getProductId().equals(request.getProductId()))
                .findFirst()
                .map(item -> new WishlistItemResult(item, false))
                .orElseGet(() -> new WishlistItemResult(createWishListItem(wishlist, request), true));
    }

    private void updateWishlistItemQuantity(WishlistItemResult wishlistItemResult, int quantityToAdd) {
        if (wishlistItemResult.wishlistItem != null && !wishlistItemResult.isNew) {
            wishlistItemResult.wishlistItem.setQuantity(wishlistItemResult.wishlistItem.getQuantity() + quantityToAdd);
        }
    }

    private Wishlist getAuthenticatedUserWishlist(HttpServletRequest request) {
        Long userId = userService.getUserId(request);
        return getWishListByUser(userId);
    }

    private Wishlist getWishListByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ItemNotFoundException(USER_NOT_FOUND);
        }
        return wishlistRepository.findByUser_UserId(userId).orElseGet(() -> createAndSaveNewWishlist(userId));
    }

    private Wishlist createAndSaveNewWishlist(Long userId) {
        User user = userService.getUserById(userId);
        Wishlist newWishlist = Wishlist.builder()
                .user(user)
                .wishlistItems(new ArrayList<>())
                .build();
        return wishlistRepository.save(newWishlist);
    }

    private WishlistItemId setWishlistItemId(Long wishlistId, Long productId) {
        WishlistItemId wishlistItemId = new WishlistItemId();
        wishlistItemId.setWishlist(wishlistId);
        wishlistItemId.setProduct(productId);
        return wishlistItemId;
    }

    private void saveWishlistAndItem(Wishlist wishlist, WishlistItem wishlistItem) {
        wishlistRepository.save(wishlist);
        wishlistItemRepository.save(wishlistItem);
    }

    private Map<Long, WishlistItem> buildExistingItemsMap(Wishlist wishlist) {
        return wishlist
                .getWishlistItems()
                .stream()
                .collect(Collectors.toMap(item -> item.getProduct().getProductId(), item -> item));
    }
}
