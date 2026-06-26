package com.playerstock.trading_platform;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class TradeService {
    @Autowired private PlayerRepository      playerRepository;
    @Autowired private UserRepository        userRepository;
    @Autowired private PortfolioRepository   portfolioRepository;
    @Autowired private PriceHistoryRepository priceHistoryRepository;
    @Autowired private TradeLogRepository    tradeLogRepository;

    private double bondingCurvePrice(double ipoPrice, int netSupply) {
        double adjustment = netSupply >= 0
            ? 0.1 * Math.sqrt(netSupply)
            : -0.1 * Math.sqrt(-netSupply);
        return Math.max(0.01, ipoPrice + adjustment);
    }

    private PortfolioItem getOrCreateItem(Long userId, Long playerId, User user, Player player) {
        return portfolioRepository.findByUserIdAndPlayerId(userId, playerId)
            .orElse(new PortfolioItem(user, player, 0));
    }

    private void saveOrDelete(PortfolioItem item) {
        if (item.getSharesOwned() == 0 && item.getSharesShorted() == 0) {
            portfolioRepository.delete(item);
        } else {
            portfolioRepository.save(item);
        }
    }

    private void logTrade(User user, Player player, String type,
                          int quantity, double total, Double realizedPnl) {
        double   perShare = quantity > 0 ? total / quantity : 0;
        TradeLog log      = new TradeLog(user, player, type, quantity, perShare, total);
        log.setRealizedPnl(realizedPnl);
        tradeLogRepository.save(log);
        priceHistoryRepository.save(new PriceHistory(player, player.getCurrentPrice()));
    }

    // ── BUY ──────────────────────────────────────────────────────────────────
    @Transactional
    public void buyStock(Long userId, Long playerId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        BigDecimal totalCost = BigDecimal.ZERO;
        int supply = player.getOutstandingShares();
        for (int i = 0; i < quantity; i++) {
            totalCost = totalCost.add(BigDecimal.valueOf(bondingCurvePrice(player.getIpoPrice(), supply)));
            supply++;
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        if (user.getBalance().compareTo(totalCost) < 0)
            throw new IllegalArgumentException("Insufficient balance. Required: $" + totalCost + ", Available: $" + user.getBalance());

        user.setBalance(user.getBalance().subtract(totalCost));
        player.setOutstandingShares(player.getOutstandingShares() + quantity);

        PortfolioItem item = getOrCreateItem(userId, playerId, user, player);
        // Update weighted average cost basis
        double prevCost     = item.getAvgLongCost() * item.getSharesOwned();
        double newCost      = totalCost.doubleValue();
        int    newTotal     = item.getSharesOwned() + quantity;
        item.setAvgLongCost((prevCost + newCost) / newTotal);
        item.setSharesOwned(newTotal);

        userRepository.save(user);
        playerRepository.save(player);
        portfolioRepository.save(item);
        logTrade(user, player, "buy", quantity, totalCost.doubleValue(), null);
    }

    // ── SELL ─────────────────────────────────────────────────────────────────
    @Transactional
    public void sellStock(Long userId, Long playerId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        PortfolioItem item = portfolioRepository.findByUserIdAndPlayerId(userId, playerId)
            .orElseThrow(() -> new IllegalStateException("You do not own any shares of this player."));
        if (item.getSharesOwned() < quantity)
            throw new IllegalStateException("You cannot sell more shares than you own.");

        BigDecimal totalCredit = BigDecimal.ZERO;
        int supply = player.getOutstandingShares();
        for (int i = 0; i < quantity; i++) {
            supply--;
            totalCredit = totalCredit.add(BigDecimal.valueOf(bondingCurvePrice(player.getIpoPrice(), supply)));
        }
        totalCredit = totalCredit.setScale(2, RoundingMode.HALF_UP);

        double realizedPnl = totalCredit.doubleValue() - (item.getAvgLongCost() * quantity);

        user.setBalance(user.getBalance().add(totalCredit));
        player.setOutstandingShares(player.getOutstandingShares() - quantity);
        item.setSharesOwned(item.getSharesOwned() - quantity);

        userRepository.save(user);
        playerRepository.save(player);
        saveOrDelete(item);
        logTrade(user, player, "sell", quantity, totalCredit.doubleValue(), realizedPnl);
    }

    // ── SHORT ────────────────────────────────────────────────────────────────
    @Transactional
    public void shortStock(Long userId, Long playerId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        BigDecimal totalProceeds = BigDecimal.ZERO;
        int supply = player.getOutstandingShares();
        for (int i = 0; i < quantity; i++) {
            supply--;
            totalProceeds = totalProceeds.add(BigDecimal.valueOf(bondingCurvePrice(player.getIpoPrice(), supply)));
        }
        totalProceeds = totalProceeds.setScale(2, RoundingMode.HALF_UP);

        user.setBalance(user.getBalance().add(totalProceeds));
        player.setOutstandingShares(player.getOutstandingShares() - quantity);

        PortfolioItem item = getOrCreateItem(userId, playerId, user, player);
        // Update weighted average short price
        double prevProceeds = item.getAvgShortPrice() * item.getSharesShorted();
        double newProceeds  = totalProceeds.doubleValue();
        int    newTotal     = item.getSharesShorted() + quantity;
        item.setAvgShortPrice((prevProceeds + newProceeds) / newTotal);
        item.setSharesShorted(newTotal);

        userRepository.save(user);
        playerRepository.save(player);
        portfolioRepository.save(item);
        logTrade(user, player, "short", quantity, totalProceeds.doubleValue(), null);
    }

    // ── COVER ────────────────────────────────────────────────────────────────
    @Transactional
    public void coverShort(Long userId, Long playerId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        PortfolioItem item = portfolioRepository.findByUserIdAndPlayerId(userId, playerId)
            .orElseThrow(() -> new IllegalStateException("You have no short position on this player."));
        if (item.getSharesShorted() < quantity)
            throw new IllegalStateException("You cannot cover more shares than you are short.");

        BigDecimal totalCost = BigDecimal.ZERO;
        int supply = player.getOutstandingShares();
        for (int i = 0; i < quantity; i++) {
            totalCost = totalCost.add(BigDecimal.valueOf(bondingCurvePrice(player.getIpoPrice(), supply)));
            supply++;
        }
        totalCost = totalCost.setScale(2, RoundingMode.HALF_UP);

        if (user.getBalance().compareTo(totalCost) < 0)
            throw new IllegalArgumentException("Insufficient balance to cover. Required: $" + totalCost);

        double realizedPnl = (item.getAvgShortPrice() * quantity) - totalCost.doubleValue();

        user.setBalance(user.getBalance().subtract(totalCost));
        player.setOutstandingShares(player.getOutstandingShares() + quantity);
        item.setSharesShorted(item.getSharesShorted() - quantity);

        userRepository.save(user);
        playerRepository.save(player);
        saveOrDelete(item);
        logTrade(user, player, "cover", quantity, totalCost.doubleValue(), realizedPnl);
    }
}
