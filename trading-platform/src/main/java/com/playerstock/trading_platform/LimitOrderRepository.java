package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {
    List<LimitOrder> findByStatus(String status);
    List<LimitOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
