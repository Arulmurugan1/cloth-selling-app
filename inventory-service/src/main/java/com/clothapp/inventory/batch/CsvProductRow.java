package com.clothapp.inventory.batch;

import java.math.BigDecimal;

public class CsvProductRow {
    private String sku;
    private String name;
    private String description;
    private String category;
    private String size;
    private String color;
    private BigDecimal price;
    private String imageUrl;
    private Integer stockQty;
    private Integer defectiveQty;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getStockQty() { return stockQty; }
    public void setStockQty(Integer stockQty) { this.stockQty = stockQty; }
    public Integer getDefectiveQty() { return defectiveQty; }
    public void setDefectiveQty(Integer defectiveQty) { this.defectiveQty = defectiveQty; }
}
