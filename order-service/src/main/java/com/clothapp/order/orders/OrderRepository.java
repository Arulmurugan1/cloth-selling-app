package com.clothapp.order.orders;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<CustomerOrder, String> {

    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    long countByStatus(OrderStatus status);

    List<CustomerOrder> findByStatusOrderByUpdatedAtAsc(OrderStatus status);
}
