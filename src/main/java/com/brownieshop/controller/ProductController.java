package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Product;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Customer-facing product pages.
 * All routes are public (no login required to browse), but session is read
 * to personalise the nav bar when a customer IS logged in.
 */
@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    // ── Product Listing (browse / search / filter) ─────────────
    @GetMapping
    public String listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double maxPrice,
            HttpSession session,
            Model model) {

        List<Product> products = productService.getAvailableProducts(search, category, maxPrice);
        List<Product> featured  = productService.getFeaturedProducts();
        List<String>  categories = productService.getAllCategories();

        model.addAttribute("products",   products);
        model.addAttribute("featured",   featured);
        model.addAttribute("categories", categories);
        model.addAttribute("search",     search);
        model.addAttribute("category",   category);
        model.addAttribute("maxPrice",   maxPrice);

        // Pass customer to nav bar (null-safe)
        model.addAttribute("customer", session.getAttribute("customer"));

        return "products/list";
    }

    // ── Single Product Detail ──────────────────────────────────
    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, HttpSession session, Model model) {
        Optional<Product> opt = productService.getById(id);
        if (opt.isEmpty() || !opt.get().isAvailable()) return "redirect:/products";

        model.addAttribute("product",  opt.get());
        model.addAttribute("featured", productService.getFeaturedProducts());
        model.addAttribute("customer", session.getAttribute("customer"));

        return "products/detail";
    }
}