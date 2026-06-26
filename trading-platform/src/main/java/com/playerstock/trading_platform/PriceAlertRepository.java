package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findByTriggeredFalse();
    List<PriceAlert> findByUserIdAndDismissedFalseOrderByCreatedAtDesc(Long userId);
}
