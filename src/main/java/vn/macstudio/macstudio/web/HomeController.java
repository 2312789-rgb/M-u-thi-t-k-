package vn.macstudio.macstudio.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.macstudio.macstudio.repository.CategoryRepository;
import vn.macstudio.macstudio.repository.ProductRepository;

@Controller
public class HomeController {
    private final ProductRepository products;
    private final CategoryRepository categories;
    public HomeController(ProductRepository products, CategoryRepository categories) { this.products = products; this.categories = categories; }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categories.findAllByOrderByNameAsc());
        model.addAttribute("featured", products.search(null, null, null, null, null, null, true, "best-selling").stream().limit(4).toList());
        model.addAttribute("arrivals", products.search(null, null, null, null, null, null, true, "newest").stream().limit(8).toList());
        return "home";
    }
}