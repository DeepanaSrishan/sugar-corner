package com.brownieshop.service;

import com.brownieshop.config.UploadConfig;
import com.brownieshop.dao.ProductDAO;
import com.brownieshop.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    @Autowired
    private ProductDAO productDAO;

    // ── Create ────────────────────────────────────────────────
    public long addProduct(Product product, MultipartFile image) {
        long id = productDAO.save(product);
        if (image != null && !image.isEmpty()) {
            String path = saveImage(id, image);
            if (path != null) productDAO.updateImage(id, path);
        }
        return id;
    }

    // ── Read ──────────────────────────────────────────────────
    public Optional<Product> getById(Long id) {
        return productDAO.findById(id);
    }

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public List<Product> getAvailableProducts(String search, String category, Double maxPrice) {
        if (search != null && !search.isBlank()) {
            return productDAO.searchByName(search.trim());
        }
        if (category != null && !category.isBlank()) {
            return productDAO.findByCategory(category);
        }
        if (maxPrice != null && maxPrice > 0) {
            return productDAO.findByMaxPrice(maxPrice);
        }
        return productDAO.findAllAvailable();
    }

    public List<Product> getFeaturedProducts() {
        return productDAO.findFeatured();
    }

    public List<String> getAllCategories() {
        return productDAO.findAllCategories();
    }

    // ── Update ────────────────────────────────────────────────
    public void updateProduct(Product product, MultipartFile image) {
        productDAO.update(product);
        if (image != null && !image.isEmpty()) {
            String path = saveImage(product.getId(), image);
            if (path != null) productDAO.updateImage(product.getId(), path);
        }
    }

    public void removeProduct(Long id) {
        productDAO.setAvailability(id, false);
    }

    public void hardDeleteProduct(Long id) {
        productDAO.deleteById(id);
    }

    // ── Image Upload ──────────────────────────────────────────
    private String saveImage(long productId, MultipartFile file) {
        try {
            // Use absolute path from UploadConfig
            Path uploadPath = UploadConfig.PRODUCTS_DIR;
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String ext      = getExtension(file.getOriginalFilename());
            String filename = "product_" + productId + "_" + UUID.randomUUID() + ext;
            Path   dest     = uploadPath.resolve(filename);

            file.transferTo(dest.toFile());

            // Return the RELATIVE URL path (used in <img src="...">)
            return UploadConfig.PRODUCTS_URL_PREFIX + filename;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }
}