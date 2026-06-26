package com.playerstock.trading_platform;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dividend_payouts", indexes = {
    @Index(columnList = "user_id, paid_at"),
    @Index(columnList = "espn_game_id")
})
public class DividendPayout {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "espn_game_id", nullable = false)
    private String espnGameId;

    private double points;
    private double rebounds;
    private double assists;
    private double steals;
    private double blocks;
    private double turnovers;

    @Column(name = "shares_long",  nullable = false)
    private int sharesLong;
    @Column(name = "shares_short", nullable = false)
    private int sharesShort;

    @Column(name = "dividend_per_share", nullable = false)
    private double dividendPerShare;

    @Column(name = "net_payout", nullable = false)
    private double netPayout; // positive = received, negative = paid out

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt = LocalDateTime.now();

    public DividendPayout() {}

    public Long          getId()               { return id; }
    public User          getUser()             { return user; }
    public void          setUser(User u)       { this.user = u; }
    public Player        getPlayer()           { return player; }
    public void          setPlayer(Player p)   { this.player = p; }
    public String        getEspnGameId()       { return espnGameId; }
    public void          setEspnGameId(String g) { this.espnGameId = g; }
    public double        getPoints()           { return points; }
    public void          setPoints(double v)   { this.points = v; }
    public double        getRebounds()         { return rebounds; }
    public void          setRebounds(double v) { this.rebounds = v; }
    public double        getAssists()          { return assists; }
    public void          setAssists(double v)  { this.assists = v; }
    public double        getSteals()           { return steals; }
    public void          setSteals(double v)   { this.steals = v; }
    public double        getBlocks()           { return blocks; }
    public void          setBlocks(double v)   { this.blocks = v; }
    public double        getTurnovers()        { return turnovers; }
    public void          setTurnovers(double v){ this.turnovers = v; }
    public int           getSharesLong()       { return sharesLong; }
    public void          setSharesLong(int v)  { this.sharesLong = v; }
    public int           getSharesShort()      { return sharesShort; }
    public void          setSharesShort(int v) { this.sharesShort = v; }
    public double        getDividendPerShare()  { return dividendPerShare; }
    public void          setDividendPerShare(double v) { this.dividendPerShare = v; }
    public double        getNetPayout()        { return netPayout; }
    public void          setNetPayout(double v){ this.netPayout = v; }
    public LocalDateTime getPaidAt()           { return paidAt; }
}
