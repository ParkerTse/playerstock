package com.playerstock.trading_platform;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    public PriceAlertController(PriceAlertService priceAlertService) {
        this.priceAlertService = priceAlertService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAlert(@AuthenticationPrincipal User user,
                                         @RequestParam Long   playerId,
                                         @RequestParam String direction,
                                         @RequestParam double targetPrice) {
        try {
            PriceAlert alert = priceAlertService.createAlert(
                user.getId(), playerId, direction, targetPrice);
            return ResponseEntity.ok(Map.of("alertId", alert.getId(), "message", "Alert set"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/dismiss/{id}")
    public ResponseEntity<String> dismissAlert(@AuthenticationPrincipal User user,
                                               @PathVariable Long id) {
        try {
            priceAlertService.dismissAlert(user.getId(), id);
            return ResponseEntity.ok("Alert dismissed");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-alerts")
    public List<Map<String, Object>> getMyAlerts(@AuthenticationPrincipal User user) {
        return priceAlertService.getMyAlerts(user.getId()).stream()
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",          a.getId());
                m.put("playerName",  a.getPlayer().getName());
                m.put("playerImage", a.getPlayer().getImageUrl());
                m.put("playerId",    a.getPlayer().getId());
                m.put("direction",   a.getDirection());
                m.put("targetPrice", a.getTargetPrice());
                m.put("triggered",   a.isTriggered());
                m.put("createdAt",   a.getCreatedAt().toString());
                m.put("triggeredAt", a.getTriggeredAt() != null ? a.getTriggeredAt().toString() : null);
                return m;
            })
            .collect(Collectors.toList());
    }
}
