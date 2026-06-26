package com.playerstock.trading_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_log", indexes = @Index(columnList = "executed_at"))
public class TradeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String type; // buy | sell | short | cover

    @Column(nullable = false)
    private int quantity;

    @Column(name = "price_per_share", nullable = false)
    private double pricePerShare;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    @Column(name = "realized_pnl")
    private Double realizedPnl; // null for buy/short; populated for sell/cover

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    public TradeLog() {}

    public TradeLog(User user, Player player, String type, int quantity, double pricePerShare, double totalAmount) {
        this.user          = user;
        this.player        = player;
        this.type          = type;
        this.quantity      = quantity;
        this.pricePerShare = pricePerShare;
        this.totalAmount   = totalAmount;
        this.executedAt    = LocalDateTime.now();
    }

    public Long          getId()             { return id; }
    public User          getUser()           { return user; }
    public Player        getPlayer()         { return player; }
    public String        getType()           { return type; }
    public int           getQuantity()       { return quantity; }
    public double        getPricePerShare()  { return pricePerShare; }
    public double        getTotalAmount()    { return totalAmount; }
    public Double        getRealizedPnl()    { return realizedPnl; }
    public void          setRealizedPnl(Double v) { this.realizedPnl = v; }
    public LocalDateTime getExecutedAt()     { return executedAt; }
}
