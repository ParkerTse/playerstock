package com.playerstock.trading_platform;
import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String position;

    @Column
    private String team;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "ipo_price", nullable = false)
    private double ipoPrice = 10.00;

    @Column(name = "outstanding_shares", nullable = false)
    private int outstandingShares = 0;

    private static final double SCALE_FACTOR = 0.1;

    public Player() {}

    public Player(String name) {
        this.name = name;
    }

    public Player(String name, String position, String team) {
        this.name     = name;
        this.position = position;
        this.team     = team;
    }

    // Price is symmetric around ipoPrice — positive net supply pushes price up,
    // negative (more shorts than longs) pushes price below ipoPrice, floored at $0.01
    public double getCurrentPrice() {
        int net = this.outstandingShares;
        double adjustment = net >= 0
            ? SCALE_FACTOR * Math.sqrt(net)
            : -SCALE_FACTOR * Math.sqrt(-net);
        return Math.max(0.01, ipoPrice + adjustment);
    }

    public Long   getId()              { return id; }
    public String getName()            { return name; }
    public void   setName(String name) { this.name = name; }
    public String getPosition()        { return position; }
    public void   setPosition(String p){ this.position = p; }
    public String getTeam()            { return team; }
    public void   setTeam(String t)    { this.team = t; }
    public String getImageUrl()        { return imageUrl; }
    public void   setImageUrl(String u){ this.imageUrl = u; }
    public double getIpoPrice()            { return ipoPrice; }
    public void   setIpoPrice(double p)    { this.ipoPrice = p; }
    public int    getOutstandingShares()              { return outstandingShares; }
    public void   setOutstandingShares(int shares)    { this.outstandingShares = shares; }
}