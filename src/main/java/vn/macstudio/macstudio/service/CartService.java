package vn.macstudio.macstudio.service;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import vn.macstudio.macstudio.model.Product;
import vn.macstudio.macstudio.repository.ProductRepository;
import vn.macstudio.macstudio.web.form.CartLine;

@Service
public class CartService {
    private static final String SESSION_KEY = "store.cart";
    private final ProductRepository products;

    public CartService(ProductRepository products) { this.products = products; }

    public List<CartLine> get(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (!(value instanceof List<?> list)) return new ArrayList<>();
        return list.stream().filter(CartLine.class::isInstance).map(CartLine.class::cast)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public void add(HttpSession session, long productId, int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        Product product = products.findById(productId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (!"Đang bán".equals(product.getStatus())) throw new IllegalArgumentException("Sản phẩm hiện không được mở bán.");
        if (product.getQuantity() < 1) throw new IllegalArgumentException("Sản phẩm hiện đã hết hàng.");
        List<CartLine> cart = get(session);
        int index = -1;
        for (int i = 0; i < cart.size(); i++) if (cart.get(i).productId().equals(productId)) { index = i; break; }
        int nextQuantity = quantity + (index < 0 ? 0 : cart.get(index).quantity());
        if (nextQuantity > product.getQuantity()) throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho.");
        CartLine line = new CartLine(productId, product.getName(), product.getImageUrl(), product.getPrice(), product.getSize(), product.getColor(), nextQuantity);
        if (index < 0) cart.add(line); else cart.set(index, line);
        save(session, cart);
    }

    public void update(HttpSession session, long productId, int quantity) {
        List<CartLine> cart = get(session);
        int index = -1;
        for (int i = 0; i < cart.size(); i++) if (cart.get(i).productId().equals(productId)) { index = i; break; }
        if (index < 0) return;
        if (quantity <= 0) { cart.remove(index); save(session, cart); return; }
        Product product = products.findById(productId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
        if (quantity > product.getQuantity()) throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho.");
        cart.set(index, cart.get(index).withQuantity(quantity));
        save(session, cart);
    }

    public void remove(HttpSession session, long productId) {
        List<CartLine> cart = get(session);
        cart.removeIf(line -> line.productId().equals(productId));
        save(session, cart);
    }

    public void clear(HttpSession session) { session.removeAttribute(SESSION_KEY); }

    private void save(HttpSession session, List<CartLine> cart) {
        session.setAttribute(SESSION_KEY, new ArrayList<>(cart));
    }
}