package vn.macstudio.macstudio.web;

import java.math.BigDecimal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.macstudio.macstudio.repository.CategoryRepository;
import vn.macstudio.macstudio.repository.ProductRepository;

@Controller
@RequestMapping("/products")
public class ProductsController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    public ProductsController(ProductRepository products, CategoryRepository categories) { this.products = products; this.categories = categories; }

    @GetMapping
    public String index(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String size, @RequestParam(required = false) String color,
                        @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(defaultValue = "newest") String sort, Model model) {
        model.addAttribute("products", products.search(blankToNull(q), categoryId, blankToNull(size), blankToNull(color), minPrice, maxPrice, false, sort));
        model.addAttribute("categories", categories.findAllByOrderByNameAsc());
        model.addAttribute("q", q); model.addAttribute("categoryId", categoryId); model.addAttribute("size", size);
        model.addAttribute("color", color); model.addAttribute("minPrice", minPrice); model.addAttribute("maxPrice", maxPrice); model.addAttribute("sort", sort);
        return "products/index";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable long id, Model model) {
        var product = products.findById(id).orElse(null);
        if (product == null) return "error/404";
        model.addAttribute("product", product);
        return "products/details";
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}