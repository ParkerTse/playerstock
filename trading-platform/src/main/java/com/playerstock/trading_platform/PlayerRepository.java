package com.playerstock.trading_platform;

import com.playerstock.trading_platform.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    boolean existsByName(String name);
    java.util.Optional<Player> findByName(String name);
}