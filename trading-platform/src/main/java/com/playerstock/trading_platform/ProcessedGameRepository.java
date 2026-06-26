package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedGameRepository extends JpaRepository<ProcessedGame, String> {}
