package com.playerstock.trading_platform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history", indexes = @Index(columnList = "player_id, recorded_at"))
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    @JsonIgnore
    private Player player;

    @Column(nullable = false)
    private double price;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public PriceHistory() {}

    public PriceHistory(Player player, double price) {
        this.player     = player;
        this.price      = price;
        this.recordedAt = LocalDateTime.now();
    }

    public Long          getId()          { return id; }
    public double        getPrice()       { return price; }
    public LocalDateTime getRecordedAt()  { return recordedAt; }
}
