package com.playerstock.trading_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
public class PriceAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String direction; // ABOVE, BELOW

    @Column(name = "target_price", nullable = false)
    private double targetPrice;

    private boolean triggered = false;
    private boolean dismissed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    public PriceAlert() {}

    public Long          getId()           { return id; }
    public User          getUser()         { return user; }
    public void          setUser(User u)   { this.user = u; }
    public Player        getPlayer()       { return player; }
    public void          setPlayer(Player p) { this.player = p; }
    public String        getDirection()    { return direction; }
    public void          setDirection(String d) { this.direction = d; }
    public double        getTargetPrice()  { return targetPrice; }
    public void          setTargetPrice(double v) { this.targetPrice = v; }
    public boolean       isTriggered()     { return triggered; }
    public void          setTriggered(boolean v) { this.triggered = v; }
    public boolean       isDismissed()     { return dismissed; }
    public void          setDismissed(boolean v) { this.dismissed = v; }
    public LocalDateTime getCreatedAt()    { return createdAt; }
    public LocalDateTime getTriggeredAt()  { return triggeredAt; }
    public void          setTriggeredAt(LocalDateTime t) { this.triggeredAt = t; }
}
