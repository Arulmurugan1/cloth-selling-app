package com.clothapp.order.orders;

import com.clothapp.order.orders.OrderDtos.DashboardDto;
import com.clothapp.order.orders.OrderDtos.OrderDto;
import com.clothapp.order.orders.OrderDtos.PlaceOrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto place(@RequestHeader("X-User-Id") String userId,
                          @RequestHeader("X-User-Name") String userName,
                          @RequestHeader(value = "X-User-Email", required = false) String userEmail,
                          @Valid @RequestBody PlaceOrderRequest body) {
        return service.place(userId, userName, userEmail, body);
    }

    @GetMapping
    public List<OrderDto> myOrders(@RequestHeader("X-User-Id") String userId,
                                   @RequestHeader("X-User-Role") String role) {
        if ("ADMIN".equals(role) || "SUPER_ADMIN".equals(role)) {
            return service.all();
        }
        return service.byCustomer(userId);
    }

    @GetMapping("/{orderId}")
    public OrderDto one(@PathVariable String orderId) {
        return service.one(orderId);
    }

    @GetMapping("/dashboard/summary")
    public DashboardDto dashboard() {
        return service.dashboard();
    }
}
