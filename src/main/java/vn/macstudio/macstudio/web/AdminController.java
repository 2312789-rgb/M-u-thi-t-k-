package vn.macstudio.macstudio.web;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.macstudio.macstudio.model.Category;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.model.Product;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CategoryRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.OrderItemRepository;
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.service.OrderService;
import vn.macstudio.macstudio.web.form.AdminProductForm;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final AppUserRepository users;
    private final CustomerOrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderService orderService;
    private final java.nio.file.Path uploadPath;

    public AdminController(ProductRepository products, CategoryRepository categories, AppUserRepository users,
                           CustomerOrderRepository orders, OrderItemRepository orderItems, OrderService orderService,
                           @org.springframework.beans.factory.annotation.Value("${store.upload-dir:uploads}") String uploadDirectory) {
        this.products = products; this.categories = categories; this.users = users; this.orders = orders;
        this.orderItems = orderItems; this.orderService = orderService;
        this.uploadPath = java.nio.file.Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @GetMapping
    public String dashboard(Model model) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime week = today.minusDays(6);
        model.addAttribute("productCount", products.count()); model.addAttribute("customerCount", users.countByRole(Role.CUSTOMER));
        model.addAttribute("orderCount", orders.count()); model.addAttribute("todayOrders", orders.countByOrderDateGreaterThanEqual(today));
        model.addAttribute("revenue", orders.revenueByStatus(OrderStatus.DELIVERED));
        model.addAttribute("lowStock", products.countByQuantityLessThanEqual(5));
        model.addAttribute("bestSellers", products.findTop5ByOrderBySoldCountDesc());
        model.addAttribute("recentRevenue", orders.revenueSince(week, OrderStatus.CANCELLED));
        return "admin/dashboard";
    }

    @GetMapping("/products")
    public String products(Model model) { model.addAttribute("products", products.findAll()); return "admin/products"; }

    @GetMapping("/products/new")
    public String newProduct(Model model) {
        model.addAttribute("form", new AdminProductForm());
        model.addAttribute("currentImages", List.of());
        return productForm(model);
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable long id, Model model, RedirectAttributes flash) {
        Product product = products.findById(id).orElse(null);
        if (product == null) { flash.addFlashAttribute("error", "Không tìm thấy sản phẩm."); return "redirect:/admin/products"; }
        AdminProductForm form = new AdminProductForm(); form.setId(product.getId()); form.setCategoryId(product.getCategory().getId());
        form.setName(product.getName()); form.setDescription(product.getDescription()); form.setPrice(product.getPrice());
        form.setQuantity(product.getQuantity()); form.setSize(product.getSize()); form.setColor(product.getColor()); form.setStatus(product.getStatus());
        model.addAttribute("form", form); model.addAttribute("currentImages", product.getImageUrls());
        return productForm(model);
    }

    @PostMapping("/products/save")
    public String saveProduct(@Valid @ModelAttribute("form") AdminProductForm form, BindingResult errors,
                              @RequestParam(name = "images", required = false) List<MultipartFile> uploadedImages,
                              Model model, RedirectAttributes flash) {
        List<MultipartFile> images = uploadedImages == null ? List.of() :
            uploadedImages.stream().filter(image -> image != null && !image.isEmpty()).toList();
        Category category = form.getCategoryId() == null ? null : categories.findById(form.getCategoryId()).orElse(null);
        if (category == null) errors.rejectValue("categoryId", "category.invalid", "Hãy chọn danh mục hợp lệ.");
        if (images.size() > 10) errors.reject("images.count", "Mỗi sản phẩm có thể tải lên tối đa 10 ảnh trong một lần.");
        if (images.stream().anyMatch(image -> image.getSize() > 4_000_000 ||
                !List.of("image/jpeg", "image/png", "image/webp").contains(image.getContentType())))
            errors.reject("images.invalid", "Mỗi ảnh phải là JPG, PNG, WEBP và nhỏ hơn 4 MB.");
        if (errors.hasErrors()) {
            model.addAttribute("currentImages", form.getId() == null ? List.of() :
                products.findById(form.getId()).map(Product::getImageUrls).orElse(List.of()));
            return productForm(model);
        }
        Product product = form.getId() == null ? new Product() : products.findById(form.getId()).orElse(null);
        if (product == null) { flash.addFlashAttribute("error", "Không tìm thấy sản phẩm."); return "redirect:/admin/products"; }
        product.setCategory(category); product.setName(form.getName().trim()); product.setDescription(form.getDescription());
        product.setPrice(form.getPrice()); product.setQuantity(form.getQuantity()); product.setSize(form.getSize()); product.setColor(form.getColor()); product.setStatus(form.getStatus());
        if (!images.isEmpty()) {
            List<java.nio.file.Path> savedImagePaths = new ArrayList<>();
            try {
                java.nio.file.Files.createDirectories(uploadPath);
                for (MultipartFile image : images) {
                    String extension = switch (image.getContentType()) {
                        case "image/jpeg" -> ".jpg";
                        case "image/png" -> ".png";
                        default -> ".webp";
                    };
                    String fileName = UUID.randomUUID() + extension;
                    java.nio.file.Path imagePath = uploadPath.resolve(fileName);
                    java.nio.file.Files.copy(image.getInputStream(), imagePath);
                    savedImagePaths.add(imagePath);
                    String imageUrl = "/uploads/" + fileName;
                    if (product.getImageUrl() == null || product.getImageUrl().isBlank()) product.setImageUrl(imageUrl);
                    else product.addImageUrl(imageUrl);
                }
            } catch (java.io.IOException exception) {
                for (java.nio.file.Path savedImagePath : savedImagePaths) {
                    try {
                        java.nio.file.Files.deleteIfExists(savedImagePath);
                    } catch (java.io.IOException cleanupException) {
                        exception.addSuppressed(cleanupException);
                    }
                }
                log.error("Unable to store uploaded product images", exception);
                flash.addFlashAttribute("error", "Không thể lưu đầy đủ ảnh tải lên. Vui lòng kiểm tra thư mục uploads rồi thử lại.");
                return "redirect:/admin/products";
            }
        } else if (product.getImageUrl() == null) product.setImageUrl("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=900&q=85");
        products.save(product); flash.addFlashAttribute("success", "Đã lưu sản phẩm."); return "redirect:/admin/products";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable long id, RedirectAttributes flash) {
        Product product = products.findById(id).orElse(null);
        if (product == null) flash.addFlashAttribute("error", "Không tìm thấy sản phẩm.");
        else if (orderItems.existsByProductId(id)) flash.addFlashAttribute("error", "Sản phẩm đã có trong đơn hàng, không thể xóa.");
        else { products.delete(product); flash.addFlashAttribute("success", "Đã xóa sản phẩm."); }
        return "redirect:/admin/products";
    }

    @GetMapping("/categories")
    public String categories(Model model) { model.addAttribute("categories", categories.findAllByOrderByNameAsc()); model.addAttribute("category", new Category("", "")); return "admin/categories"; }

    @PostMapping("/categories/save")
    public String saveCategory(@RequestParam(required = false) Long id, @RequestParam String name, @RequestParam(required = false) String description, RedirectAttributes flash) {
        Category category = id == null ? new Category(name.trim(), description) : categories.findById(id).orElse(null);
        if (category == null) { flash.addFlashAttribute("error", "Không tìm thấy danh mục."); return "redirect:/admin/categories"; }
        category.setName(name.trim()); category.setDescription(description); categories.save(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable long id, RedirectAttributes flash) {
        if (products.existsByCategoryId(id)) flash.addFlashAttribute("error", "Danh mục còn sản phẩm, chưa thể xóa."); else categories.deleteById(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/orders")
    public String orders(Model model) { model.addAttribute("orders", orders.findAllByOrderByOrderDateDesc()); model.addAttribute("statuses", OrderStatus.values()); return "admin/orders"; }

    @PostMapping("/orders/{id}/status")
    public String changeStatus(@PathVariable long id, @RequestParam OrderStatus status, RedirectAttributes flash) {
        try {
            orderService.changeStatus(id, status);
            flash.addFlashAttribute("success", "Đã cập nhật trạng thái đơn hàng.");
        } catch (IllegalArgumentException exception) { flash.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/admin/orders";
    }

    @GetMapping("/customers")
    public String customers(Model model) { model.addAttribute("customers", users.findAllByRoleOrderByCreatedDateDesc(Role.CUSTOMER)); return "admin/customers"; }

    @GetMapping("/customers/{id}")
    public String customer(@PathVariable long id, Model model) {
        var customer = users.findById(id).orElse(null); if (customer == null || customer.getRole() != Role.CUSTOMER) return "error/404";
        model.addAttribute("customer", customer); model.addAttribute("orders", orders.findAllByUserEmailIgnoreCaseOrderByOrderDateDesc(customer.getEmail())); return "admin/customer";
    }

    @PostMapping("/customers/{id}/save")
    public String saveCustomer(@PathVariable long id, @RequestParam String fullName, @RequestParam String phone, @RequestParam String address, RedirectAttributes flash) {
        var customer = users.findById(id).orElse(null); if (customer == null) return "redirect:/admin/customers";
        customer.setFullName(fullName.trim()); customer.setPhone(phone.trim()); customer.setAddress(address.trim()); users.save(customer);
        flash.addFlashAttribute("success", "Đã cập nhật khách hàng."); return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable long id, RedirectAttributes flash) {
        var customer = users.findById(id).orElse(null);
        if (customer != null && customer.getRole() == Role.CUSTOMER) {
            if (!orders.findAllByUserEmailIgnoreCaseOrderByOrderDateDesc(customer.getEmail()).isEmpty()) flash.addFlashAttribute("error", "Khách hàng có lịch sử đơn hàng, không thể xóa.");
            else { users.delete(customer); flash.addFlashAttribute("success", "Đã xóa khách hàng."); }
        }
        return "redirect:/admin/customers";
    }

    private String productForm(Model model) {
        model.addAttribute("categories", categories.findAllByOrderByNameAsc());
        return "admin/product-form";
    }
}