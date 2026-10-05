package vn.macstudio.macstudio.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.pattern.payment.PaymentFactory;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.service.CartService;
import vn.macstudio.macstudio.service.OrderService;
import vn.macstudio.macstudio.web.form.CartLine;
import vn.macstudio.macstudio.web.form.CheckoutForm;

@Controller
@RequestMapping("/cart")
public class CartController {
    private final CartService cart;
    private final OrderService orders;
    private final PaymentFactory payments;
    private final CustomerOrderRepository orderRepository;
    public CartController(CartService cart, OrderService orders, PaymentFactory payments, CustomerOrderRepository orderRepository) {
        this.cart = cart; this.orders = orders; this.payments = payments; this.orderRepository = orderRepository;
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        List<CartLine> lines = cart.get(session);
        model.addAttribute("cart", lines);
        model.addAttribute("total", lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
        return "cart/index";
    }

    @PostMapping("/add")
    public String add(@RequestParam long id, @RequestParam(defaultValue = "1") int quantity, HttpSession session, RedirectAttributes flash) {
        try { cart.add(session, id, quantity); flash.addFlashAttribute("success", "Đã thêm sản phẩm vào giỏ hàng."); }
        catch (IllegalArgumentException exception) { flash.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam long id, @RequestParam int quantity, HttpSession session, RedirectAttributes flash) {
        try { cart.update(session, id, quantity); }
        catch (IllegalArgumentException exception) { flash.addFlashAttribute("error", exception.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam long id, HttpSession session) { cart.remove(session, id); return "redirect:/cart"; }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, @AuthenticationPrincipal AppUser user, Model model) {
        if (cart.get(session).isEmpty()) return "redirect:/cart";
        CheckoutForm form = new CheckoutForm(); form.setRecipientName(user.getFullName()); form.setPhone(user.getPhone()); form.setAddress(user.getAddress());
        addCheckoutModel(session, model); model.addAttribute("form", form); return "cart/checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult errors,
                           HttpSession session, @AuthenticationPrincipal AppUser user, Model model, RedirectAttributes flash) {
        List<CartLine> lines = cart.get(session);
        if (lines.isEmpty()) return "redirect:/cart";
        if (errors.hasErrors()) { addCheckoutModel(session, model); return "cart/checkout"; }
        try {
            var order = orders.placeOrder(user.getEmail(), form, lines);
            String instructions = payments.create(order.getPaymentMethod()).instructions(order.getId(), order.getTotalAmount());
            cart.clear(session);
            flash.addFlashAttribute("paymentInstructions", instructions);
            return "redirect:/cart/confirmation/" + order.getId();
        } catch (IllegalArgumentException exception) {
            flash.addFlashAttribute("error", exception.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/confirmation/{id}")
    public String confirmation(@org.springframework.web.bind.annotation.PathVariable long id, @AuthenticationPrincipal AppUser user, Model model) {
        var optional = orderRepository.findByIdAndUserEmailIgnoreCase(id, user.getEmail()).orElse(null);
        if (optional == null) return "error/404";
        model.addAttribute("order", optional);
        return "cart/confirmation";
    }

    private void addCheckoutModel(HttpSession session, Model model) {
        List<CartLine> lines = cart.get(session);
        model.addAttribute("cart", lines);
        model.addAttribute("total", lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}