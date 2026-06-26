package com.playerstock.trading_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "processed_games")
public class ProcessedGame {
    @Id
    private String espnGameId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    public ProcessedGame() {}
    public ProcessedGame(String espnGameId) { this.espnGameId = espnGameId; }

    public String        getEspnGameId()  { return espnGameId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
