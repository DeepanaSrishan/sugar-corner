package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Product;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired
    private ProductService productService;

    // ── Guard ──────────────────────────────────────────────────
    private boolean isAdmin(HttpSession session) {
        Customer c = (Customer) session.getAttribute("customer");
        return c != null && c.isAdmin();
    }

    // ── LIST ───────────────────────────────────────────────────
    @GetMapping
    public String listProducts(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("admin",    session.getAttribute("customer"));
        return "admin/products";
    }

    // ── ADD FORM ───────────────────────────────────────────────
    @GetMapping("/add")
    public String showAddForm(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        model.addAttribute("product", new Product());
        return "admin/product-form";
    }

    // ── ADD SUBMIT ─────────────────────────────────────────────
    /**
     * FIX: description is required=false (textarea can be empty).
     * FIX: featured and available are String not boolean.
     *      HTML checkboxes send NO parameter at all when unchecked —
     *      Spring throws MissingServletRequestParameterException if you use boolean.
     *      A checked checkbox sends the value "on", unchecked sends nothing (null).
     */
    @PostMapping("/add")
    public String addProduct(
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam String category,
            @RequestParam BigDecimal price,
            @RequestParam int stockQuantity,
            @RequestParam(required = false) String featured,
            @RequestParam(required = false) String available,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/admin/login";

        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setPrice(price);
        p.setStockQuantity(stockQuantity);
        p.setFeatured("on".equals(featured));
        p.setAvailable("on".equals(available));

        productService.addProduct(p, image);
        return "redirect:/admin/products?success=added";
    }

    // ── EDIT FORM ──────────────────────────────────────────────
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";

        Optional<Product> opt = productService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/products";

        model.addAttribute("product", opt.get());
        model.addAttribute("editMode", true);
        return "admin/product-form";
    }

    // ── EDIT SUBMIT ────────────────────────────────────────────
    @PostMapping("/edit/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam String category,
            @RequestParam BigDecimal price,
            @RequestParam int stockQuantity,
            @RequestParam(required = false) String featured,
            @RequestParam(required = false) String available,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpSession session) {

        if (!isAdmin(session)) return "redirect:/admin/login";

        Optional<Product> opt = productService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/products";

        Product p = opt.get();
        p.setName(name);
        p.setDescription(description);
        p.setCategory(category);
        p.setPrice(price);
        p.setStockQuantity(stockQuantity);
        p.setFeatured("on".equals(featured));
        p.setAvailable("on".equals(available));

        productService.updateProduct(p, image);
        return "redirect:/admin/products?success=updated";
    }

    // ── REMOVE (soft delete – marks unavailable) ───────────────
    @PostMapping("/remove/{id}")
    public String removeProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        productService.removeProduct(id);
        return "redirect:/admin/products?success=removed";
    }

    // ── HARD DELETE ────────────────────────────────────────────
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        productService.hardDeleteProduct(id);
        return "redirect:/admin/products?success=deleted";
    }
}