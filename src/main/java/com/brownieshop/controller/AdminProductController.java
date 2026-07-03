package com.brownieshop.controller;

import com.brownieshop.model.Customer;
import com.brownieshop.model.Product;
import com.brownieshop.service.OrderService;
import com.brownieshop.service.ProductService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    @Autowired private ProductService productService;
    @Autowired private OrderService   orderService;

    private Customer getAdmin(HttpSession session) {
        Customer admin = (Customer) session.getAttribute("adminUser");
        if (admin != null && admin.isAdmin()) return admin;
        Customer fallback = (Customer) session.getAttribute("customer");
        if (fallback != null && fallback.isAdmin()) return fallback;
        return null;
    }

    private boolean isAdmin(HttpSession session) {
        Customer c = getAdmin(session);
        return c != null && c.isAdmin();
    }

    private void addCommon(Model model) {
        model.addAttribute("notifCount", orderService.getPendingOrderCount());
    }

    // ── All Products ──────────────────────────────────────────
    // AUTO-DETECT: every time admin opens the products page,
    // the top 4 most-ordered products are auto-marked as featured.
    // This keeps popular products always up-to-date without manual work.
    @GetMapping
    public String listProducts(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(model);

        // Auto-update featured based on order data (top 4 most ordered)
        List<Long> autoFeaturedIds = productService.autoUpdateFeatured(4);

        List<Product> products = productService.getAllProducts();

        // Add ordered count to model as a map: productId -> orderedCount
        // Used in the template to show "Ordered X times" per product
        Map<Long, Integer> orderedCounts = new HashMap<>();
        for (Product p : products) {
            orderedCounts.put(p.getId(), productService.getOrderedCount(p.getId()));
        }

        model.addAttribute("products",        products);
        model.addAttribute("orderedCounts",   orderedCounts);
        model.addAttribute("autoFeaturedIds", autoFeaturedIds);
        return "admin/products";
    }

    // ── Manual "Auto-detect Popular" trigger ─────────────────
    // Called by the 🔄 button in the admin products page.
    // Returns JSON so the page can update without full reload.
    @PostMapping("/auto-popular")
    @ResponseBody
    public Map<String, Object> triggerAutoPopular(
            @RequestParam(defaultValue = "4") int topN,
            HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        if (!isAdmin(session)) { resp.put("error", "Not authorised"); return resp; }

        List<Long> ids = productService.autoUpdateFeatured(topN);
        resp.put("success", true);
        resp.put("featuredIds", ids);
        resp.put("count", ids.size());
        resp.put("message", ids.isEmpty()
                ? "No order data yet — no products auto-featured."
                : "✅ " + ids.size() + " most-ordered products marked as Popular!");
        return resp;
    }

    // ── Add form ──────────────────────────────────────────────
    @GetMapping("/add")
    public String showAddForm(HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(model);
        model.addAttribute("product", new Product());
        return "admin/product-form";
    }

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

    // ── Edit form ─────────────────────────────────────────────
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        addCommon(model);
        Optional<Product> opt = productService.getById(id);
        if (opt.isEmpty()) return "redirect:/admin/products";
        model.addAttribute("product",       opt.get());
        model.addAttribute("editMode",      true);
        model.addAttribute("orderedCount",  productService.getOrderedCount(id));
        return "admin/product-form";
    }

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

    @PostMapping("/remove/{id}")
    public String removeProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        productService.removeProduct(id);
        return "redirect:/admin/products?success=removed";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/admin/login";
        productService.hardDeleteProduct(id);
        return "redirect:/admin/products?success=deleted";
    }
}