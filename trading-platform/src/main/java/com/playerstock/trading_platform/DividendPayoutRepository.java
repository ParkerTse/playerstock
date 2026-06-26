package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DividendPayoutRepository extends JpaRepository<DividendPayout, Long> {
    List<DividendPayout> findByUserIdOrderByPaidAtDesc(Long userId);
    List<DividendPayout> findByEspnGameId(String espnGameId);
}
