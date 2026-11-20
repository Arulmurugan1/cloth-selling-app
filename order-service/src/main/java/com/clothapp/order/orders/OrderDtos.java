package com.clothapp.order.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record PlaceOrderRequest(@NotEmpty List<@Valid OrderItemRequest> items) {
    }

    public record OrderItemRequest(@NotBlank String sku, @Min(1) int qty) {
    }

    public record OrderLineDto(String sku, String productName, BigDecimal unitPrice, int qty, BigDecimal lineTotal)
            implements Serializable {
        static OrderLineDto from(OrderLine l) {
            return new OrderLineDto(l.getSku(), l.getProductName(), l.getUnitPrice(), l.getQty(), l.lineTotal());
        }
    }

    public record OrderDto(String id, String customerId, String customerName, String customerEmail,
                           OrderStatus status, BigDecimal orderTotal, Instant createdAt, Instant updatedAt,
                           List<OrderLineDto> lines) implements Serializable {

        static OrderDto from(CustomerOrder o) {
            return new OrderDto(o.getId(), o.getCustomerId(), o.getCustomerName(), o.getCustomerEmail(),
                    o.getStatus(), o.getOrderTotal(), o.getCreatedAt(), o.getUpdatedAt(),
                    o.getLines().stream().map(OrderLineDto::from).toList());
        }
    }

    public record DashboardDto(long totalOrders, long customers, long deliveredToday, long yetToShip) {
    }
}
