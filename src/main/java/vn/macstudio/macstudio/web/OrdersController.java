package vn.macstudio.macstudio.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;

@Controller
@RequestMapping("/orders")
public class OrdersController {
    private final CustomerOrderRepository orders;
    public OrdersController(CustomerOrderRepository orders) { this.orders = orders; }
    @GetMapping public String index(@AuthenticationPrincipal AppUser user, Model model) {
        model.addAttribute("orders", orders.findAllByUserEmailIgnoreCaseOrderByOrderDateDesc(user.getEmail())); return "orders/index";
    }
    @GetMapping("/{id}") public String details(@PathVariable long id, @AuthenticationPrincipal AppUser user, Model model) {
        var order = orders.findByIdAndUserEmailIgnoreCase(id, user.getEmail()).orElse(null);
        if (order == null) return "error/404";
        model.addAttribute("order", order); return "orders/details";
    }
}