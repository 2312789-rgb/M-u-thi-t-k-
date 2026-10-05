package vn.macstudio.macstudio.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

public class AdminProductForm {
    private Long id;
    @NotNull private Long categoryId;
    @NotBlank @Size(max = 140) private String name;
    @Size(max = 2000) private String description;
    @NotNull @DecimalMin(value = "1000", message = "Giá sản phẩm phải từ 1.000đ trở lên.") private BigDecimal price;
    @PositiveOrZero private int quantity;
    @Size(max = 20) private String size;
    @Size(max = 40) private String color;
    @Size(max = 30) private String status = "Đang bán";
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}