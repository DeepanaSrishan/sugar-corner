package com.brownieshop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Maps URL paths to static resource locations.
 *
 * THE BUG THAT WAS HERE:
 *   When you override addResourceHandlers(), Spring Boot STOPS applying
 *   its own default static-resource mapping (classpath:/static/).
 *   The original code only registered /uploads/** and /static/**,
 *   so a request to /videos/bg-video.mp4 returned 404 because
 *   no handler covered the root /** path from classpath:/static/.
 *
 * THE FIX:
 *   Added a /** handler that points back to classpath:/static/.
 *   This restores the default Spring Boot behaviour so any file placed
 *   in src/main/resources/static/ is served at its direct URL:
 *     /videos/bg-video.mp4  → static/videos/bg-video.mp4  ✅
 *     /css/style.css        → static/css/style.css         ✅
 *   The /uploads/** and /static/** handlers are kept as before.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // ── 1. Uploaded images (outside classpath) ──────────────────
        // Maps /uploads/** → <project-root>/uploads/ on disk.
        String uploadsAbsolutePath = Paths.get("uploads")
                .toAbsolutePath()
                .toUri()
                .toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsAbsolutePath + "/");

        // ── 2. Explicit /static/** prefix (kept for compatibility) ──
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // ── 3. FIX: Default static handler ─────────────────────────
        // Restores Spring Boot's default behaviour that was lost when
        // we overrode addResourceHandlers().
        // Maps /** → classpath:/static/ so that:
        //   /videos/bg-video.mp4  works  (was returning 404 before this fix)
        //   /css/x.css            works
        //   /js/x.js              works
        // Must be registered LAST so /uploads/** and /static/** are
        // matched first for their specific paths.
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}