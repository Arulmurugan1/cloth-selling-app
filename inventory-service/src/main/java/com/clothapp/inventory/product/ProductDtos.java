package com.clothapp.inventory.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductDto(String id, String sku, String name, String description, String category, String size,
                             String color, BigDecimal price, String imageUrl, int stockQty, int reservedQty,
                             int releasedQty, int defectiveQty, int availableQty, boolean active, boolean defective,
                             boolean saleable) implements Serializable {

        public static ProductDto from(Product p) {
            return new ProductDto(p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getCategory(),
                    p.getSize(), p.getColor(), p.getPrice(), p.getImageUrl(), p.getStockQty(), p.getReservedQty(),
                    p.getReleasedQty(), p.getDefectiveQty(), p.availableQty(), p.isActive(), p.isDefective(),
                    p.saleable());
        }
    }

    public record ProductRequest(
            @NotBlank @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9._-]+", message = "sku may contain letters, digits, '.', '_' and '-'") String sku,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String category,
            @Size(max = 30) String size,
            @Size(max = 50) String color,
            @NotNull @DecimalMin("0.00") BigDecimal price,
            @Size(max = 500) @Pattern(regexp = "^(https?://|/).*$|^$", message = "imageUrl must be http(s) or a relative path") String imageUrl,
            @Min(0) Integer stockQty) {
    }

    public record InventoryUpdate(@NotNull @Min(0) Integer stockQty, @Min(0) Integer defectiveQty) {
    }

    public record ActiveUpdate(@NotNull Boolean active) {
    }

    public record DefectiveUpdate(@NotNull Boolean defective) {
    }

    public record StockLine(@NotBlank String sku, @NotNull @Min(1) Integer qty) {
    }

    public record StockRequest(@NotNull @Size(min = 1) List<@jakarta.validation.Valid StockLine> items) {
    }

    public record ReservedLine(String sku, String name, BigDecimal unitPrice, int qty) {
    }

    public record ReserveResult(boolean reserved, List<String> problems, List<ReservedLine> lines) {
    }
}
