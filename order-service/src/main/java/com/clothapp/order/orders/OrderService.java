package com.clothapp.order.orders;

import com.clothapp.order.common.ConflictException;
import com.clothapp.order.common.NotFoundException;
import com.clothapp.order.orders.InventoryClient.StockLine;
import com.clothapp.order.orders.OrderDtos.DashboardDto;
import com.clothapp.order.orders.OrderDtos.OrderDto;
import com.clothapp.order.orders.OrderDtos.OrderItemRequest;
import com.clothapp.order.orders.OrderDtos.PlaceOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    public static final String CACHE = "orders";

    private final OrderRepository repository;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository repository, InventoryClient inventoryClient) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    public OrderDto place(String userId, String userName, String userEmail, PlaceOrderRequest request) {
        List<StockLine> stock = request.items().stream()
                .collect(Collectors.groupingBy(OrderItemRequest::sku, Collectors.summingInt(OrderItemRequest::qty)))
                .entrySet().stream()
                .map(e -> new StockLine(e.getKey(), e.getValue()))
                .toList();

        var reserve = inventoryClient.reserve(stock);
        if (reserve == null || !reserve.reserved()) {
            throw new ConflictException(reserve == null ? "Inventory reservation failed" : String.join("; ", reserve.problems()));
        }

        CustomerOrder order = new CustomerOrder(userId, userName, userEmail);
        reserve.lines().forEach(l -> order.addLine(new OrderLine(l.sku(), l.name(), l.unitPrice(), l.qty())));
        repository.save(order);
        log.info("Order {} placed for customer {} with {} line(s)", order.getId(), userId, order.getLines().size());
        return OrderDto.from(order);
    }

    @Cacheable(cacheNames = CACHE, key = "'by-customer:' + #userId")
    @Transactional(readOnly = true)
    public List<OrderDto> byCustomer(String userId) {
        return repository.findByCustomerIdOrderByCreatedAtDesc(userId).stream().map(OrderDto::from).toList();
    }

    @Cacheable(cacheNames = CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<OrderDto> all() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(OrderDto::from).toList();
    }

    @Cacheable(cacheNames = CACHE, key = "'one:' + #orderId")
    @Transactional(readOnly = true)
    public OrderDto one(String orderId) {
        return OrderDto.from(repository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId)));
    }

    @Cacheable(cacheNames = CACHE, key = "'dashboard'")
    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        long total = repository.count();
        long deliveredToday = repository.findByStatusOrderByUpdatedAtAsc(OrderStatus.DELIVERED).stream()
                .filter(o -> o.getUpdatedAt().atOffset(ZoneOffset.UTC).toLocalDate()
                        .equals(Instant.now().atOffset(ZoneOffset.UTC).toLocalDate()))
                .count();
        long yetToShip = repository.countByStatus(OrderStatus.ORDER_PLACED)
                + repository.countByStatus(OrderStatus.PICKING)
                + repository.countByStatus(OrderStatus.SHIPPING);
        long customers = repository.findAllByOrderByCreatedAtDesc().stream().map(CustomerOrder::getCustomerId)
                .collect(Collectors.toSet()).size();
        return new DashboardDto(total, customers, deliveredToday, yetToShip);
    }

    @CacheEvict(cacheNames = CACHE, allEntries = true)
    @Transactional
    @Scheduled(fixedRate = 20_000L, initialDelay = 20_000L)
    public void progressLifecycle() {
        Instant now = Instant.now();
        progress(OrderStatus.ORDER_PLACED, OrderStatus.PICKING, now, false);
        progress(OrderStatus.PICKING, OrderStatus.SHIPPING, now, true);
        progress(OrderStatus.SHIPPING, OrderStatus.DELIVERED, now, false);
    }

    private void progress(OrderStatus from, OrderStatus to, Instant now, boolean commitInventory) {
        List<CustomerOrder> candidates = repository.findByStatusOrderByUpdatedAtAsc(from).stream()
                .filter(o -> ChronoUnit.SECONDS.between(o.getUpdatedAt(), now) >= 20)
                .toList();
        for (CustomerOrder order : candidates) {
            if (commitInventory) {
                List<StockLine> lines = order.getLines().stream()
                        .map(l -> new StockLine(l.getSku(), l.getQty()))
                        .toList();
                inventoryClient.commit(lines);
            }
            order.setStatus(to);
            log.info("Order {} status {} -> {}", order.getId(), from, to);
        }
    }
}
