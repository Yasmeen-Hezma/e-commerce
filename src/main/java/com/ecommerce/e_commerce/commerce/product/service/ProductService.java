package com.ecommerce.e_commerce.commerce.product.service;

import com.ecommerce.e_commerce.commerce.cart.model.CartItem;
import com.ecommerce.e_commerce.common.dto.PaginatedResponse;
import com.ecommerce.e_commerce.commerce.product.dto.ProductRequest;
import com.ecommerce.e_commerce.commerce.product.dto.ProductResponse;
import com.ecommerce.e_commerce.commerce.product.dto.StockWarning;
import com.ecommerce.e_commerce.commerce.product.model.Product;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    PaginatedResponse<ProductResponse> searchProducts(Long brandId, Long categoryId, String productName, Integer status, Pageable pageable);

    Product getNonDeletedProductById(Long id);

    Optional<StockWarning>  checkStockAndWarn(Product product, int requested);

    void checkStockAvailability(List<CartItem> cartItems);

    void decreaseStock(Long productId, int quantity);
}
