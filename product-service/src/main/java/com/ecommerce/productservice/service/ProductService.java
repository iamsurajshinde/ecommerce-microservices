package com.ecommerce.productservice.service;

import com.ecommerce.productservice.event.EventPublisher;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final EventPublisher eventPublisher;

    @Value("${product.low-stock-threshold:5}")
    private int lowStockThreshold;

    public Page<Product> findProducts(String keyword, String category, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
        }
        if (category != null && !category.isBlank()) {
            return productRepository.findByCategory_NameIgnoreCase(category, pageable);
        }
        return productRepository.findAll(pageable);
    }

    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategory_Id(categoryId, pageable);
    }

    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    public Product create(Product product) {
        validateProduct(product);
        return productRepository.save(product);
    }

    public Product update(Long id, Product changes) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        validateProduct(changes);
        product.setName(changes.getName());
        product.setDescription(changes.getDescription());
        product.setPrice(changes.getPrice());
        product.setStockQuantity(changes.getStockQuantity());
        product.setCategory(changes.getCategory());
        return productRepository.save(product);
    }

    @Transactional
    public Product decreaseStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Stock decrease quantity must be positive.");
        }
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
            throw new IllegalStateException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
        Product savedProduct = productRepository.save(product);
        publishLowStockIfNeeded(savedProduct);
        return savedProduct;
    }

    private void publishLowStockIfNeeded(Product product) {
        Integer remaining = product.getStockQuantity();
        if (remaining == null || remaining > lowStockThreshold) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        // Include remaining stock in the id so each threshold crossing is a distinct event.
        payload.put("eventId", "low-stock-" + product.getId() + "-" + remaining);
        payload.put("productId", product.getId());
        payload.put("productName", product.getName());
        payload.put("remainingStock", remaining);
        eventPublisher.publish(EventPublisher.RK_LOW_STOCK, payload);
    }

    @Transactional
    public Product increaseStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Stock increase quantity must be positive.");
        }
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));
        int currentStock = product.getStockQuantity() == null ? 0 : product.getStockQuantity();
        product.setStockQuantity(currentStock + quantity);
        return productRepository.save(product);
    }

    private void validateProduct(Product product) {
        if (product == null || product.getName() == null || product.getName().isBlank()
                || product.getPrice() == null || !Double.isFinite(product.getPrice())
                || product.getPrice() < 0
                || product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Product name, non-negative price, and stock are required.");
        }
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new IllegalArgumentException("Product not found.");
        }
        productRepository.deleteById(id);
    }
}
