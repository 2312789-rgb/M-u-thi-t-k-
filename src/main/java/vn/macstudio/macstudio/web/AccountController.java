package vn.macstudio.macstudio.web;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.web.form.ProfileForm;
import vn.macstudio.macstudio.web.form.RegisterForm;

@Controller
@RequestMapping("/account")
public class AccountController {
    private final AppUserRepository users;
    private final PasswordEncoder passwords;
    private final CustomerOrderRepository orders;
    public AccountController(AppUserRepository users, PasswordEncoder passwords, CustomerOrderRepository orders) {
        this.users = users; this.passwords = passwords; this.orders = orders;
    }

    @GetMapping("/login")
    public String login() { return "account/login"; }

    @GetMapping("/register")
    public String register(Model model) { model.addAttribute("form", new RegisterForm()); return "account/register"; }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors, Model model) {
        if (!form.getPassword().equals(form.getConfirmPassword())) errors.rejectValue("confirmPassword", "password.mismatch", "Mật khẩu xác nhận không khớp.");
        if (users.findByEmailIgnoreCase(form.getEmail()).isPresent()) errors.rejectValue("email", "email.exists", "Email đã được sử dụng.");
        if (errors.hasErrors()) return "account/register";
        users.save(new AppUser(form.getFullName().trim(), form.getEmail().trim(), passwords.encode(form.getPassword()), form.getPhone().trim(), "", Role.CUSTOMER));
        return "redirect:/account/login?registered";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AppUser user, Model model) {
        ProfileForm form = new ProfileForm(); form.setFullName(user.getFullName()); form.setPhone(user.getPhone()); form.setAddress(user.getAddress());
        model.addAttribute("form", form); model.addAttribute("recentOrders", orders.findTop5ByUserEmailIgnoreCaseOrderByOrderDateDesc(user.getEmail()));
        return "account/profile";
    }

    @PostMapping("/profile")
    public String profile(@Valid @ModelAttribute("form") ProfileForm form, BindingResult errors,
                          @AuthenticationPrincipal AppUser user, Model model, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            model.addAttribute("recentOrders", orders.findTop5ByUserEmailIgnoreCaseOrderByOrderDateDesc(user.getEmail()));
            return "account/profile";
        }
        user.setFullName(form.getFullName().trim()); user.setPhone(form.getPhone().trim()); user.setAddress(form.getAddress().trim());
        users.save(user);
        flash.addFlashAttribute("success", "Đã cập nhật thông tin cá nhân.");
        return "redirect:/account/profile";
    }
}