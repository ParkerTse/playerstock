package com.playerstock.trading_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "limit_orders", indexes = @Index(columnList = "status"))
public class LimitOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String type; // buy, sell, short, cover

    @Column(name = "target_price", nullable = false)
    private double targetPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, FILLED, CANCELLED, FAILED

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    public LimitOrder() {}

    public Long          getId()          { return id; }
    public User          getUser()        { return user; }
    public void          setUser(User u)  { this.user = u; }
    public Player        getPlayer()      { return player; }
    public void          setPlayer(Player p) { this.player = p; }
    public String        getType()        { return type; }
    public void          setType(String t) { this.type = t; }
    public double        getTargetPrice() { return targetPrice; }
    public void          setTargetPrice(double v) { this.targetPrice = v; }
    public int           getQuantity()    { return quantity; }
    public void          setQuantity(int q) { this.quantity = q; }
    public String        getStatus()      { return status; }
    public void          setStatus(String s) { this.status = s; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public LocalDateTime getFilledAt()    { return filledAt; }
    public void          setFilledAt(LocalDateTime t) { this.filledAt = t; }
}
