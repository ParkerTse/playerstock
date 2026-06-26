package com.playerstock.trading_platform;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
    List<TradeLog> findAllByOrderByExecutedAtDesc(Pageable pageable);
    List<TradeLog> findByUserIdOrderByExecutedAtDesc(Long userId);
    List<TradeLog> findByPlayerIdOrderByExecutedAtDesc(Long playerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT t.player.id, SUM(t.quantity) FROM TradeLog t " +
        "WHERE t.executedAt > :since GROUP BY t.player.id")
    List<Object[]> sumVolumeByPlayerAfter(
        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);
}
