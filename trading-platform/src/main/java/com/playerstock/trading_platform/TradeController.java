package com.playerstock.trading_platform;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/trades")
@CrossOrigin(origins = "*")
public class TradeController {

    private final TradeService       tradeService;
    private final TradeLogRepository tradeLogRepository;

    public TradeController(TradeService tradeService, TradeLogRepository tradeLogRepository) {
        this.tradeService       = tradeService;
        this.tradeLogRepository = tradeLogRepository;
    }

    @PostMapping("/buy")
    public String buyStock(@AuthenticationPrincipal User user,
                           @RequestParam Long playerId, @RequestParam int quantity) {
        try   { tradeService.buyStock(user.getId(), playerId, quantity); return "Trade executed successfully!"; }
        catch (Exception e) { return "Trade failed: " + e.getMessage(); }
    }

    @PostMapping("/sell")
    public String sellStock(@AuthenticationPrincipal User user,
                            @RequestParam Long playerId, @RequestParam int quantity) {
        try   { tradeService.sellStock(user.getId(), playerId, quantity); return "Trade executed successfully!"; }
        catch (Exception e) { return "Trade failed: " + e.getMessage(); }
    }

    @PostMapping("/short")
    public String shortStock(@AuthenticationPrincipal User user,
                             @RequestParam Long playerId, @RequestParam int quantity) {
        try   { tradeService.shortStock(user.getId(), playerId, quantity); return "Trade executed successfully!"; }
        catch (Exception e) { return "Trade failed: " + e.getMessage(); }
    }

    @PostMapping("/cover")
    public String coverShort(@AuthenticationPrincipal User user,
                             @RequestParam Long playerId, @RequestParam int quantity) {
        try   { tradeService.coverShort(user.getId(), playerId, quantity); return "Trade executed successfully!"; }
        catch (Exception e) { return "Trade failed: " + e.getMessage(); }
    }

    @GetMapping("/my-history")
    public List<Map<String, Object>> getMyHistory(@AuthenticationPrincipal User user) {
        List<TradeLog> logs = tradeLogRepository.findByUserIdOrderByExecutedAtDesc(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (TradeLog log : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("playerName",    log.getPlayer().getName());
            entry.put("playerImage",   log.getPlayer().getImageUrl());
            entry.put("playerId",      log.getPlayer().getId());
            entry.put("type",          log.getType());
            entry.put("quantity",      log.getQuantity());
            entry.put("pricePerShare", log.getPricePerShare());
            entry.put("totalAmount",   log.getTotalAmount());
            entry.put("realizedPnl",   log.getRealizedPnl());
            entry.put("executedAt",    log.getExecutedAt().toString());
            result.add(entry);
        }
        return result;
    }

    @GetMapping("/feed")
    public List<Map<String, Object>> getTradeFeed() {
        List<TradeLog> logs = tradeLogRepository.findAllByOrderByExecutedAtDesc(PageRequest.of(0, 30));
        List<Map<String, Object>> feed = new ArrayList<>();
        for (TradeLog log : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("username",      log.getUser().getUsername());
            entry.put("playerName",    log.getPlayer().getName());
            entry.put("playerImage",   log.getPlayer().getImageUrl());
            entry.put("type",          log.getType());
            entry.put("quantity",      log.getQuantity());
            entry.put("pricePerShare", log.getPricePerShare());
            entry.put("totalAmount",   log.getTotalAmount());
            entry.put("executedAt",    log.getExecutedAt().toString());
            feed.add(entry);
        }
        return feed;
    }
}
