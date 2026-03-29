package com.brownieshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Maps the /uploads/** URL path to the actual "uploads/" folder on disk.
 *
 * WHY THIS IS NEEDED:
 *   - Spring Boot only serves files from src/main/resources/static/ by default.
 *   - Uploaded images are saved to uploads/ folder at the project root (outside classpath).
 *   - Without this config, the browser gets 404 for every uploaded image.
 *
 * HOW IT WORKS:
 *   - When browser requests  GET /uploads/products/product_1_abc.jpg
 *   - Spring looks inside    <project-root>/uploads/products/product_1_abc.jpg
 *   - And serves it directly as a static resource.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Resolve the absolute path of the uploads folder
        // Paths.get("uploads").toAbsolutePath() = C:/Users/you/brownie-shop/uploads  (Windows)
        //                                       = /home/you/brownie-shop/uploads      (Linux/Mac)
        String uploadsAbsolutePath = Paths.get("uploads")
                .toAbsolutePath()
                .toUri()
                .toString();

        // Map URL /uploads/** → disk folder uploads/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsAbsolutePath + "/");

        // Also keep Spring's default static resource handling working
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}