package com.playerstock.trading_platform;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByPlayerIdOrderByRecordedAtAsc(Long playerId, Pageable pageable);
}
