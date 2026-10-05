package vn.macstudio.macstudio.data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.macstudio.macstudio.model.AppUser;
import vn.macstudio.macstudio.model.Category;
import vn.macstudio.macstudio.model.CustomerOrder;
import vn.macstudio.macstudio.model.OrderItem;
import vn.macstudio.macstudio.model.OrderStatus;
import vn.macstudio.macstudio.model.PaymentMethod;
import vn.macstudio.macstudio.model.Product;
import vn.macstudio.macstudio.model.Role;
import vn.macstudio.macstudio.repository.AppUserRepository;
import vn.macstudio.macstudio.repository.CategoryRepository;
import vn.macstudio.macstudio.repository.CustomerOrderRepository;
import vn.macstudio.macstudio.repository.ProductRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final AppUserRepository users;
    private final CustomerOrderRepository orders;
    private final PasswordEncoder passwords;
    private final String adminEmail;
    private final String adminPassword;
    private final boolean seedDemoUsers;

    public DataInitializer(CategoryRepository categories, ProductRepository products, AppUserRepository users,
                           CustomerOrderRepository orders, PasswordEncoder passwords,
                           @Value("${store.admin-email}") String adminEmail,
                           @Value("${store.admin-password}") String adminPassword,
                           @Value("${store.seed-demo-users:true}") boolean seedDemoUsers) {
        this.categories = categories; this.products = products; this.users = users; this.orders = orders; this.passwords = passwords;
        this.adminEmail = adminEmail; this.adminPassword = adminPassword; this.seedDemoUsers = seedDemoUsers;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categories.count() == 0) categories.saveAll(List.of(new Category("Áo", "Áo mặc hằng ngày"), new Category("Quần", "Quần dài và quần ngắn"),
            new Category("Váy", "Chân váy"), new Category("Đầm", "Đầm liền"), new Category("Áo khoác", "Lớp khoác ngoài"), new Category("Phụ kiện", "Phụ kiện hoàn thiện trang phục")));
        if (products.count() == 0) seedProducts();
        seedUser(adminEmail, "Admin Mặc Studio", adminPassword, Role.ADMIN);
        if (seedDemoUsers) {
            for (int i = 1; i <= 5; i++) seedUser("khach" + i + "@example.com", "Khách hàng " + i, "Customer123!", Role.CUSTOMER);
            if (orders.count() == 0) seedOrders();
        }
    }

    private void seedProducts() {
        List<String> names = List.of("Áo thun cotton form rộng", "Áo sơ mi linen", "Áo polo dệt kim", "Áo tank top rib",
            "Quần jeans straight", "Quần ống suông", "Quần short chino", "Quần cargo nhẹ", "Chân váy midi xếp ly", "Chân váy denim",
            "Đầm cổ vuông", "Đầm linen dáng dài", "Blazer relaxed fit", "Áo khoác denim", "Cardigan len mỏng", "Túi tote canvas", "Thắt lưng da tối giản", "Khăn lụa họa tiết");
        long[] categoryIds = {1,1,1,1,2,2,2,2,3,3,4,4,5,5,5,6,6,6};
        String[] imageIds = {"1521572163474-6864f9cf17ab", "1596755094514-f87e34085b2c", "1627225924765-552d49cf47ad", "1603252109303-2751441dd157",
            "1542272604-787c3835535d", "1594633312681-425c7b97ccd1", "1624378439575-d8705ad7ae80", "1624378439575-d8705ad7ae80",
            "1583496661160-fb5886a0aaaa", "1542272604-787c3835535d", "1595777457583-95e059d581b8", "1566174053879-31528523f8ae",
            "1591047139829-d91aecb6caea", "1551028719-00167b16eac5", "1576566588028-4147f3842f27", "1544816155-12df9643f363", "", ""};
        for (int i = 0; i < names.size(); i++) {
            Product product = new Product();
            product.setCategory(categories.findById(categoryIds[i]).orElseThrow());
            product.setName(names.get(i)); product.setDescription("Thiết kế dễ phối, chất liệu chọn lọc cho nhịp sống hằng ngày.");
            product.setPrice(BigDecimal.valueOf(259000L + i * 47000L)); product.setQuantity(18 + i % 12);
            product.setSize(i >= 15 ? "One size" : new String[]{"S", "M", "L", "XL"}[i % 4]);
            product.setColor(new String[]{"Đen", "Trắng ngà", "Xanh navy", "Be"}[i % 4]);
            product.setStatus("Đang bán"); product.setCreatedDate(LocalDateTime.now().minusDays(i));
            product.setImageUrl(i == 16 ? "/images/leather-belt.jpg" : i == 17 ? "/images/silk-scarf.jpg" : "https://images.unsplash.com/photo-" + imageIds[i] + "?auto=format&fit=crop&w=900&q=85");
            products.save(product);
        }
    }

    private void seedUser(String email, String name, String password, Role role) {
        if (users.findByEmailIgnoreCase(email).isEmpty()) users.save(new AppUser(name, email, passwords.encode(password), "0900000000", "12 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh", role));
    }

    private void seedOrders() {
        List<AppUser> customers = users.findAllByRoleOrderByCreatedDateDesc(Role.CUSTOMER);
        List<Product> sampleProducts = products.findAll().stream().limit(4).toList();
        OrderStatus[] statuses = {OrderStatus.PENDING, OrderStatus.DELIVERED, OrderStatus.DELIVERED, OrderStatus.SHIPPING};
        for (int i = 0; i < 4; i++) {
            Product product = sampleProducts.get(i);
            product.setQuantity(Math.max(0, product.getQuantity() - 1));
            product.setSoldCount(product.getSoldCount() + 1);
            CustomerOrder order = new CustomerOrder();
            order.setUser(customers.get(i));
            order.setRecipientName(customers.get(i).getFullName());
            order.setPhone(customers.get(i).getPhone());
            order.setShippingAddress(customers.get(i).getAddress());
            order.setPaymentMethod(PaymentMethod.COD);
            order.setStatus(statuses[i]);
            order.setOrderDate(LocalDateTime.now().minusDays(i));
            OrderItem item = new OrderItem(product, 1, product.getPrice());
            order.addItem(item);
            order.setTotalAmount(item.getSubTotal());
            orders.save(order);
        }
    }
}