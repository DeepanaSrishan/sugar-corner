package com.brownieshop.config;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralizes upload path resolution.
 *
 * WHY: Paths.get("uploads") is RELATIVE to the JVM working directory.
 * In IntelliJ that is usually the project root, which is correct.
 * But to be safe and consistent, we resolve it to an absolute path once here.
 *
 * RESULT: uploads/ folder always lives at <project-root>/uploads/
 */
@Component
public class UploadConfig {

    // Absolute path of the uploads root folder
    public static final Path UPLOAD_ROOT = Paths.get("uploads").toAbsolutePath();

    public static final Path PRODUCTS_DIR      = UPLOAD_ROOT.resolve("products");
    public static final Path PROFILE_PHOTOS_DIR = UPLOAD_ROOT.resolve("profile-photos");

    // URL prefixes that map to these folders (via WebMvcConfig)
    public static final String PRODUCTS_URL_PREFIX       = "uploads/products/";
    public static final String PROFILE_PHOTOS_URL_PREFIX = "uploads/profile-photos/";
}