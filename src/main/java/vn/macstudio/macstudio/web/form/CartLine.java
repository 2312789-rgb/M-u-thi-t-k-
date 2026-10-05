package vn.macstudio.macstudio.web.form;

import java.math.BigDecimal;
import java.io.Serializable;

public record CartLine(Long productId, String productName, String imageUrl, BigDecimal price, String size, String color, int quantity) implements Serializable {
    public BigDecimal subtotal() { return price.multiply(BigDecimal.valueOf(quantity)); }
    public CartLine withQuantity(int nextQuantity) { return new CartLine(productId, productName, imageUrl, price, size, color, nextQuantity); }
}