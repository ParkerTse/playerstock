package com.playerstock.trading_platform;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class LimitOrderController {

    private final LimitOrderService limitOrderService;

    public LimitOrderController(LimitOrderService limitOrderService) {
        this.limitOrderService = limitOrderService;
    }

    @PostMapping("/place")
    public ResponseEntity<?> placeOrder(@AuthenticationPrincipal User user,
                                        @RequestParam Long   playerId,
                                        @RequestParam String type,
                                        @RequestParam double targetPrice,
                                        @RequestParam int    quantity) {
        try {
            LimitOrder order = limitOrderService.placeOrder(
                user.getId(), playerId, type, targetPrice, quantity);
            return ResponseEntity.ok(Map.of("orderId", order.getId(), "message", "Limit order placed"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<String> cancelOrder(@AuthenticationPrincipal User user,
                                              @PathVariable Long id) {
        try {
            limitOrderService.cancelOrder(user.getId(), id);
            return ResponseEntity.ok("Order cancelled");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my-orders")
    public List<Map<String, Object>> getMyOrders(@AuthenticationPrincipal User user) {
        return limitOrderService.getMyOrders(user.getId()).stream()
            .map(o -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",          o.getId());
                m.put("playerName",  o.getPlayer().getName());
                m.put("playerImage", o.getPlayer().getImageUrl());
                m.put("playerId",    o.getPlayer().getId());
                m.put("type",        o.getType());
                m.put("targetPrice", o.getTargetPrice());
                m.put("quantity",    o.getQuantity());
                m.put("status",      o.getStatus());
                m.put("createdAt",   o.getCreatedAt().toString());
                m.put("filledAt",    o.getFilledAt() != null ? o.getFilledAt().toString() : null);
                return m;
            })
            .collect(Collectors.toList());
    }
}
