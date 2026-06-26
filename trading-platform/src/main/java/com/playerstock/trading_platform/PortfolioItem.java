package com.playerstock.trading_platform;

import jakarta.persistence.*;

@Entity
@Table(name = "portfolio_items")
public class PortfolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "shares_owned", nullable = false)
    private int sharesOwned;

    @Column(name = "shares_shorted", nullable = false, columnDefinition = "integer default 0")
    private int sharesShorted = 0;

    @Column(name = "avg_long_cost")
    private Double avgLongCost = 0.0;

    @Column(name = "avg_short_price")
    private Double avgShortPrice = 0.0;

    public PortfolioItem() {}

    public PortfolioItem(User user, Player player, int sharesOwned) {
        this.user = user;
        this.player = player;
        this.sharesOwned = sharesOwned;
    }

    //getters and setters
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }
    public int  getSharesOwned()             { return sharesOwned; }
    public void setSharesOwned(int s)        { this.sharesOwned = s; }
    public int    getSharesShorted()            { return sharesShorted; }
    public void   setSharesShorted(int s)       { this.sharesShorted = s; }
    public double getAvgLongCost()              { return avgLongCost  != null ? avgLongCost  : 0.0; }
    public void   setAvgLongCost(double v)      { this.avgLongCost   = v; }
    public double getAvgShortPrice()            { return avgShortPrice != null ? avgShortPrice : 0.0; }
    public void   setAvgShortPrice(double v)    { this.avgShortPrice  = v; }
}