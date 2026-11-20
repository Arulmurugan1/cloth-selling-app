package com.clothapp.inventory.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "size_label", length = 30)
    private String size;

    @Column(length = 50)
    private String color;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "released_qty", nullable = false)
    private int releasedQty;

    @Column(name = "defective_qty", nullable = false)
    private int defectiveQty;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean defective;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Product() {
    }

    public Product(String sku) {
        this.sku = sku;
    }

    public int availableQty() {
        return Math.max(0, stockQty - reservedQty - defectiveQty);
    }

    public boolean saleable() {
        return active && !defective && availableQty() > 0;
    }

    public void applyDetails(String name, String description, String category, String size, String color,
                             BigDecimal price, String imageUrl) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.size = size;
        this.color = color;
        this.price = price;
        this.imageUrl = imageUrl;
        touch();
    }

    public void setStock(int stockQty, Integer defectiveQty) {
        this.stockQty = stockQty;
        if (defectiveQty != null) {
            this.defectiveQty = defectiveQty;
        }
        touch();
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public void setDefective(boolean defective) {
        this.defective = defective;
        touch();
    }

    public void reserve(int qty) {
        this.reservedQty += qty;
        touch();
    }

    public void release(int qty) {
        int released = Math.min(qty, reservedQty);
        this.reservedQty -= released;
        this.releasedQty += released;
        touch();
    }

    public void commit(int qty) {
        int shipped = Math.min(qty, reservedQty);
        this.reservedQty -= shipped;
        this.stockQty = Math.max(0, stockQty - shipped);
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public int getStockQty() { return stockQty; }
    public int getReservedQty() { return reservedQty; }
    public int getReleasedQty() { return releasedQty; }
    public int getDefectiveQty() { return defectiveQty; }
    public boolean isActive() { return active; }
    public boolean isDefective() { return defective; }
    public Instant getUpdatedAt() { return updatedAt; }
}
