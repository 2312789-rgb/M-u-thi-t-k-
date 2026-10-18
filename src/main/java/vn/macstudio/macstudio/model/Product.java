package vn.macstudio.macstudio.model;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Size(max = 140)
    @Column(nullable = false, length = 140)
    private String name;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @PositiveOrZero
    @Column(nullable = false)
    private int quantity;

    @Column(length = 20)
    private String size;

    @Column(length = 40)
    private String color;

    @Column(length = 500)
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "display_order")
    @Column(name = "image_url", nullable = false, length = 500)
    private List<String> additionalImageUrls = new ArrayList<>();

    @Column(nullable = false, length = 30)
    private String status = "Đang bán";

    @Column(nullable = false)
    private int soldCount;

    @Column(nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    public Product() {}
    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
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
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getAdditionalImageUrls() { return additionalImageUrls; }
    public List<String> getImageUrls() {
        LinkedHashSet<String> imageUrls = new LinkedHashSet<>();
        if (imageUrl != null && !imageUrl.isBlank()) imageUrls.add(imageUrl);
        imageUrls.addAll(additionalImageUrls);
        return List.copyOf(imageUrls);
    }
    public void addImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || imageUrl.equals(this.imageUrl) || additionalImageUrls.contains(imageUrl)) return;
        additionalImageUrls.add(imageUrl);
    }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
}