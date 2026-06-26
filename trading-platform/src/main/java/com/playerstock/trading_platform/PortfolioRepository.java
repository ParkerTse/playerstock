package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<PortfolioItem, Long> {
    // Custom finder to locate a specific stock asset holding
    Optional<PortfolioItem> findByUserIdAndPlayerId(Long userId, Long playerId);
    
    List<PortfolioItem> findByUserId(Long userId);
    List<PortfolioItem> findByPlayerId(Long playerId);

    @org.springframework.data.jpa.repository.Query(
        "SELECT pi FROM PortfolioItem pi JOIN FETCH pi.user " +
        "WHERE pi.player.id = :playerId AND pi.sharesOwned > 0 " +
        "ORDER BY pi.sharesOwned DESC")
    List<PortfolioItem> findTopLongHolders(
        @org.springframework.data.repository.query.Param("playerId") Long playerId,
        org.springframework.data.domain.Pageable pageable);

}