package com.brownieshop.service;

import com.brownieshop.config.UploadConfig;
import com.brownieshop.dao.CustomerDAO;
import com.brownieshop.model.Customer;
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
public class CustomerService {

    @Autowired
    private CustomerDAO customerDAO;

    // ── Profile ───────────────────────────────────────────────
    public Optional<Customer> getById(Long id) {
        return customerDAO.findById(id);
    }

    public void updateProfile(Customer c) {
        customerDAO.updateProfile(c);
    }

    /**
     * Saves the uploaded photo to disk and updates the DB record.
     * Returns the relative URL path saved, or null if upload failed.
     */
    public String uploadProfilePhoto(Long customerId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            // Use absolute path from UploadConfig
            Path uploadPath = UploadConfig.PROFILE_PHOTOS_DIR;
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            String ext      = getExtension(file.getOriginalFilename());
            String filename = "customer_" + customerId + "_" + UUID.randomUUID() + ext;
            Path   dest     = uploadPath.resolve(filename);

            file.transferTo(dest.toFile());

            // Return relative URL path used in <img src="...">
            String relativePath = UploadConfig.PROFILE_PHOTOS_URL_PREFIX + filename;
            customerDAO.updateProfilePhoto(customerId, relativePath);
            return relativePath;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".jpg";
        return filename.substring(filename.lastIndexOf('.'));
    }

    // ── Admin ─────────────────────────────────────────────────
    public List<Customer> getAllCustomers() {
        return customerDAO.findAllCustomers();
    }

    public void blockCustomer(Long id) {
        customerDAO.updateStatus(id, "BLOCKED");
    }

    public void unblockCustomer(Long id) {
        customerDAO.updateStatus(id, "ACTIVE");
    }
}