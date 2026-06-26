package com.playerstock.trading_platform;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    private final PlayerRepository       playerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PortfolioRepository    portfolioRepository;
    private final TradeLogRepository     tradeLogRepository;

    public PlayerController(PlayerRepository playerRepository,
                            PriceHistoryRepository priceHistoryRepository,
                            PortfolioRepository portfolioRepository,
                            TradeLogRepository tradeLogRepository) {
        this.playerRepository       = playerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.portfolioRepository    = portfolioRepository;
        this.tradeLogRepository     = tradeLogRepository;
    }

    @GetMapping
    public List<Player> getAllPlayers() {
        return playerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Player getPlayer(@PathVariable Long id) {
        return playerRepository.findById(id).orElseThrow();
    }

    @GetMapping("/{id}/history")
    public List<PriceHistory> getPriceHistory(@PathVariable Long id,
                                              @RequestParam(defaultValue = "60") int limit) {
        return priceHistoryRepository.findByPlayerIdOrderByRecordedAtAsc(
            id, PageRequest.of(0, Math.min(limit, 500)));
    }

    @GetMapping("/{id}/top-holders")
    public List<Map<String, Object>> getTopHolders(@PathVariable Long id) {
        return portfolioRepository.findTopLongHolders(id, PageRequest.of(0, 5)).stream()
            .map(pi -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username",     pi.getUser().getUsername());
                m.put("sharesOwned",  pi.getSharesOwned());
                m.put("sharesShorted",pi.getSharesShorted());
                return m;
            }).collect(Collectors.toList());
    }

    @GetMapping("/{id}/trades")
    public List<Map<String, Object>> getPlayerTrades(@PathVariable Long id) {
        return tradeLogRepository.findByPlayerIdOrderByExecutedAtDesc(id, PageRequest.of(0, 15)).stream()
            .map(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username",     t.getUser().getUsername());
                m.put("type",         t.getType());
                m.put("quantity",     t.getQuantity());
                m.put("pricePerShare",t.getPricePerShare());
                m.put("executedAt",   t.getExecutedAt().toString());
                return m;
            }).collect(Collectors.toList());
    }

    @GetMapping("/by-volume")
    public List<Map<String, Object>> getByVolume() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Object[]> rows = tradeLogRepository.sumVolumeByPlayerAfter(since);

        Map<Long, Long> volumeMap = new HashMap<>();
        for (Object[] row : rows) {
            volumeMap.put((Long) row[0], ((Number) row[1]).longValue());
        }

        return playerRepository.findAll().stream()
            .sorted(Comparator.comparingLong(
                (Player p) -> volumeMap.getOrDefault(p.getId(), 0L)).reversed())
            .filter(p -> volumeMap.containsKey(p.getId()))
            .map(p -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",     p.getId());
                m.put("volume", volumeMap.get(p.getId()));
                return m;
            }).collect(Collectors.toList());
    }

    @GetMapping("/top-movers")
    public List<Player> getTopMovers() {
        return playerRepository.findAll().stream()
            .filter(p -> p.getOutstandingShares() != 0)
            .sorted(Comparator.comparingDouble(
                (Player p) -> Math.abs(p.getCurrentPrice() - p.getIpoPrice()) / p.getIpoPrice()
            ).reversed())
            .limit(10)
            .toList();
    }
}
