package com.ecommerce.e_commerce.commerce.wishlist.service;

import com.ecommerce.e_commerce.commerce.pricing.service.PricingService;
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import com.ecommerce.e_commerce.commerce.product.enums.StockWarningType;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import com.ecommerce.e_commerce.commerce.product.service.ProductService;
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
import com.ecommerce.e_commerce.common.exception.ItemNotFoundException;
import com.ecommerce.e_commerce.user.profile.model.User;
import com.ecommerce.e_commerce.user.profile.repository.UserRepository;
import com.ecommerce.e_commerce.user.profile.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.ecommerce.e_commerce.common.utils.Constants.PRODUCT_NOT_FOUND;
import static com.ecommerce.e_commerce.common.utils.Constants.USER_NOT_FOUND;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {
    @Mock
    private WishlistMapper wishlistMapper;
    @Mock
    private WishlistRepository wishlistRepository;
    @Mock
    private WishlistItemRepository wishlistItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductService productService;
    @Mock
    private WishlistItemMapper wishlistItemMapper;
    @Mock
    private UserService userService;
    @Mock
    private PricingService pricingService;
    @Mock
    private HttpServletRequest httpRequest;
    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private User user;
    private Wishlist wishlist;
    private Product product;
    private WishlistItem wishlistItem;
    private WishlistItemRequest wishlistItemRequest;
    private WishlistItemResponse wishlistItemResponse;
    private WishlistResponse wishlistResponse;

    @BeforeEach
    void setUp() {
        user = User
                .builder()
                .userId(1L)
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .phoneCode(20)
                .orders(new ArrayList<>())
                .build();

        product = Product
                .builder()
                .productId(100L)
                .productName("test product")
                .description("test description")
                .quantity(50)
                .price(BigDecimal.valueOf(99.99))
                .discount(BigDecimal.TEN)
                .deleted(false)
                .cartItems(new ArrayList<>())
                .orderItems(new ArrayList<>())
                .reviews(new ArrayList<>())
                .build();

        wishlist = Wishlist
                .builder()
                .wishlistId(1L)
                .user(user)
                .wishlistItems(new ArrayList<>())
                .build();

        wishlistItem = WishlistItem
                .builder()
                .wishlist(wishlist)
                .product(product)
                .quantity(2)
                .priceSnapshot(BigDecimal.valueOf(89.99))
                .build();

        wishlistItemRequest = new WishlistItemRequest();
        wishlistItemRequest.setProductId(100L);
        wishlistItemRequest.setQuantity(2);

        wishlistItemResponse = WishlistItemResponse
                .builder()
                .productId(100L)
                .productName("test product")
                .quantity(2)
                .originalPrice(BigDecimal.valueOf(99.99))
                .priceSnapshot(BigDecimal.valueOf(89.99))
                .discountPercent(BigDecimal.TEN)
                .lineTotal(BigDecimal.valueOf(179.98))
                .build();

        wishlistResponse = WishlistResponse
                .builder()
                .id(1L)
                .userId(1L)
                .items(List.of(wishlistItemResponse))
                .build();
    }

    @Test
    void addItemToWishlist_ShouldAddNewItem_WhenItemDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);
        when(wishlistItemMapper.toResponse(any(WishlistItem.class))).thenReturn(wishlistItemResponse);
        // Act
        WishlistItemResponse result = wishlistService.addItemToWishlist(httpRequest, wishlistItemRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(userService).getUserId(httpRequest);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(pricingService).calculatePriceSnapshot(any(Product.class));
        verify(wishlistItemRepository).save(any(WishlistItem.class));
        verify(wishlistRepository).save(wishlist);
        verify(wishlistItemMapper).toResponse(any(WishlistItem.class));
    }

    @Test
    void addItemToWishlist_ShouldIncrementQuantity_WhenItemAlreadyExists() {
        // Arrange
        wishlist.addWishlistItem(wishlistItem);

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);
        when(wishlistItemMapper.toResponse(any(WishlistItem.class))).thenAnswer(
                invocation -> {
                    WishlistItem item = invocation.getArgument(0);
                    return WishlistItemResponse
                            .builder()
                            .productId(item.getProduct().getProductId())
                            .quantity(item.getQuantity())
                            .build();
                }
        );
        wishlistItemRequest.setQuantity(3);
        // Act
        WishlistItemResponse result = wishlistService.addItemToWishlist(httpRequest, wishlistItemRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(5);
        verify(userService).getUserId(httpRequest);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(wishlistItemRepository).save(any(WishlistItem.class));
        verify(wishlistRepository).save(wishlist);
        verify(wishlistItemMapper).toResponse(any(WishlistItem.class));
    }

    @Test
    void addItemToWishlist_ShouldCreateWishlist_WhenWishlistDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(userService.getUserById(1L)).thenReturn(user);
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(wishlistItemRepository.save(any(WishlistItem.class))).thenReturn(wishlistItem);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);
        when(wishlistItemMapper.toResponse(any(WishlistItem.class))).thenReturn(wishlistItemResponse);
        // Act
        WishlistItemResponse result = wishlistService.addItemToWishlist(httpRequest, wishlistItemRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(100L);
        assertThat(result.getQuantity()).isEqualTo(2);
        verify(userService).getUserId(httpRequest);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(userService).getUserById(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(pricingService).calculatePriceSnapshot(any(Product.class));
        verify(wishlistItemRepository).save(any(WishlistItem.class));
        verify(wishlistRepository, times(2)).save(any(Wishlist.class));
        verify(wishlistItemMapper).toResponse(any(WishlistItem.class));
    }

    @Test
    void addItemToWishlist_ShouldThrowException_WhenProductNotFound() {
        // Arrange
        wishlistItemRequest.setProductId(999L);
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(productService.getNonDeletedProductById(999L))
                .thenThrow(new ItemNotFoundException(PRODUCT_NOT_FOUND));
        // Act & Assert
        assertThatThrownBy(() -> wishlistService.addItemToWishlist(httpRequest, wishlistItemRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(PRODUCT_NOT_FOUND);
        verify(userService).getUserId(httpRequest);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(999L);
        verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        verify(wishlistRepository, never()).save(any(Wishlist.class));
    }

    @Test
    void getWishlistByUser_ShouldReturnWishlist_WhenWishlistExists() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(wishlistMapper.toResponse(wishlist)).thenReturn(wishlistResponse);
        // Act
        WishlistResponse result = wishlistService.getWishlistByUser(httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(wishlistMapper).toResponse(wishlist);
    }

    @Test
    void getWishlistByUser_ShouldCreateWishlist_WhenWishlistDoesNotExist() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.empty());
        when(userService.getUserById(1L)).thenReturn(user);
        when(wishlistRepository.save(any(Wishlist.class))).thenReturn(wishlist);
        when(wishlistMapper.toResponse(any(Wishlist.class))).thenReturn(wishlistResponse);
        // Act
        WishlistResponse result = wishlistService.getWishlistByUser(httpRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(userService).getUserById(1L);
        verify(wishlistRepository).save(any(Wishlist.class));
        verify(wishlistMapper).toResponse(any(Wishlist.class));
    }

    @Test
    void getWishlistByUser_ShouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(false);
        // Act & Assert
        assertThatThrownBy(() -> wishlistService.getWishlistByUser(httpRequest))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(USER_NOT_FOUND);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository, never()).findByUser_UserId(anyLong());
    }

    @Test
    void syncWishlistSnapshot_ShouldUpdateQuantities_WhenAllProductsAvailable() {
        // Arrange
        wishlist.addWishlistItem(wishlistItem);

        WishlistItemRequest wishlistItemRequest2 = new WishlistItemRequest();
        wishlistItemRequest2.setProductId(100L);
        wishlistItemRequest2.setQuantity(5);

        UpdateWishlistItemRequest updateRequest = new UpdateWishlistItemRequest();
        updateRequest.setWishlistItems(List.of(wishlistItemRequest2));

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 5)).thenReturn(Optional.empty());
        when(wishlistMapper.toResponseWithWarnings(any(Wishlist.class), anyList()))
                .thenAnswer(invocation -> {
                    Wishlist w = invocation.getArgument(0);
                    return WishlistResponse
                            .builder()
                            .items(w.getWishlistItems()
                                    .stream()
                                    .map(item -> WishlistItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .build();
                });
        // Act
        WishlistResponse result = wishlistService.syncWishlistSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(5);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 5);
        verify(wishlistMapper).toResponseWithWarnings(any(Wishlist.class), anyList());
    }

    @Test
    void syncWishlistSnapshot_ShouldReturnWarnings_WhenStockInsufficient() {
        // Arrange
        wishlist.addWishlistItem(wishlistItem);

        WishlistItemRequest wishlistItemRequest2 = new WishlistItemRequest();
        wishlistItemRequest2.setProductId(100L);
        wishlistItemRequest2.setQuantity(500);

        UpdateWishlistItemRequest updateRequest = new UpdateWishlistItemRequest();
        updateRequest.setWishlistItems(List.of(wishlistItemRequest2));

        StockWarning warning = StockWarning
                .builder()
                .productId(100L)
                .type(StockWarningType.LIMITED_STOCK)
                .build();

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 500)).thenReturn(Optional.of(warning));
        when(wishlistMapper.toResponseWithWarnings(any(Wishlist.class), anyList()))
                .thenAnswer(invocation -> {
                    Wishlist w = invocation.getArgument(0);
                    List<StockWarning> warnings = invocation.getArgument(1);
                    return WishlistResponse
                            .builder()
                            .items(w.getWishlistItems()
                                    .stream()
                                    .map(item -> WishlistItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .warnings(warnings)
                            .build();
                });
        // Act
        WishlistResponse result = wishlistService.syncWishlistSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(2);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().getFirst().getProductId()).isEqualTo(100L);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 500);
        verify(wishlistMapper).toResponseWithWarnings(any(Wishlist.class), anyList());
    }

    @Test
    void syncWishlistSnapshot_ShouldRemoveItems_WhenNotInUpdateRequest() {
        // Arrange
        WishlistItem wishlistItem2 = WishlistItem
                .builder()
                .id(new WishlistItemId())
                .wishlist(wishlist)
                .product(Product.builder().productId(200L).price(BigDecimal.TEN).build())
                .quantity(2)
                .priceSnapshot(BigDecimal.TEN)
                .build();
        wishlist.addWishlistItem(wishlistItem);
        wishlist.addWishlistItem(wishlistItem2);

        WishlistItemRequest wishlistItemRequest2 = new WishlistItemRequest();
        wishlistItemRequest2.setProductId(100L);
        wishlistItemRequest2.setQuantity(3);

        UpdateWishlistItemRequest updateRequest = new UpdateWishlistItemRequest();
        updateRequest.setWishlistItems(List.of(wishlistItemRequest2));

        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        when(productService.getNonDeletedProductById(100L)).thenReturn(product);
        when(productService.checkStockAndWarn(product, 3)).thenReturn(Optional.empty());
        when(wishlistMapper.toResponseWithWarnings(any(Wishlist.class), anyList()))
                .thenAnswer(invocation -> {
                    Wishlist w = invocation.getArgument(0);
                    return WishlistResponse
                            .builder()
                            .items(w.getWishlistItems()
                                    .stream()
                                    .map(item -> WishlistItemResponse
                                            .builder()
                                            .productId(item.getProduct().getProductId())
                                            .quantity(item.getQuantity())
                                            .build()
                                    )
                                    .toList()
                            )
                            .build();
                });
        // Act
        WishlistResponse result = wishlistService.syncWishlistSnapshot(httpRequest, updateRequest);
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getQuantity()).isEqualTo(3);
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
        verify(productService).getNonDeletedProductById(100L);
        verify(productService).checkStockAndWarn(product, 3);
        verify(wishlistMapper).toResponseWithWarnings(any(Wishlist.class), anyList());
    }

    @Test
    void clearWishlist_ShouldRemoveAllItems_WhenWishlistHasItems() {
        // Arrange
        wishlist.addWishlistItem(wishlistItem);
        when(userService.getUserId(httpRequest)).thenReturn(1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(wishlistRepository.findByUser_UserId(1L)).thenReturn(Optional.of(wishlist));
        // Act
        wishlistService.clearWishlist(httpRequest);
        // Assert
        assertThat(wishlist.getWishlistItems()).isEmpty();
        verify(userService).getUserId(httpRequest);
        verify(userRepository).existsById(1L);
        verify(wishlistRepository).findByUser_UserId(1L);
    }
}