package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Order;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data  repository for the Order entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByCustomerId(Long customerId, Pageable pageable);
}
