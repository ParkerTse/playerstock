package com.playerstock.trading_platform;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LimitOrderService {

    private final LimitOrderRepository limitOrderRepository;
    private final UserRepository       userRepository;
    private final PlayerRepository     playerRepository;
    private final TradeService         tradeService;

    public LimitOrderService(LimitOrderRepository limitOrderRepository,
                             UserRepository userRepository,
                             PlayerRepository playerRepository,
                             TradeService tradeService) {
        this.limitOrderRepository = limitOrderRepository;
        this.userRepository       = userRepository;
        this.playerRepository     = playerRepository;
        this.tradeService         = tradeService;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void checkAndFillOrders() {
        List<LimitOrder> pending = limitOrderRepository.findByStatus("PENDING");
        for (LimitOrder order : pending) {
            try {
                double  price  = order.getPlayer().getCurrentPrice();
                boolean fill   = switch (order.getType()) {
                    case "buy",  "cover" -> price <= order.getTargetPrice();
                    case "sell", "short" -> price >= order.getTargetPrice();
                    default              -> false;
                };
                if (!fill) continue;

                Long uid = order.getUser().getId();
                Long pid = order.getPlayer().getId();
                switch (order.getType()) {
                    case "buy"   -> tradeService.buyStock(uid, pid, order.getQuantity());
                    case "sell"  -> tradeService.sellStock(uid, pid, order.getQuantity());
                    case "short" -> tradeService.shortStock(uid, pid, order.getQuantity());
                    case "cover" -> tradeService.coverShort(uid, pid, order.getQuantity());
                }
                order.setStatus("FILLED");
                order.setFilledAt(LocalDateTime.now());
            } catch (Exception e) {
                order.setStatus("FAILED");
                System.out.printf("Limit order %d failed: %s%n", order.getId(), e.getMessage());
            }
        }
    }

    @Transactional
    public LimitOrder placeOrder(Long userId, Long playerId, String type,
                                 double targetPrice, int quantity) {
        if (quantity <= 0)    throw new IllegalArgumentException("Quantity must be positive");
        if (targetPrice <= 0) throw new IllegalArgumentException("Target price must be positive");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        LimitOrder order = new LimitOrder();
        order.setUser(user);
        order.setPlayer(player);
        order.setType(type.toLowerCase());
        order.setTargetPrice(targetPrice);
        order.setQuantity(quantity);
        return limitOrderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        LimitOrder order = limitOrderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (!order.getUser().getId().equals(userId))
            throw new IllegalArgumentException("Not your order");
        if (!"PENDING".equals(order.getStatus()))
            throw new IllegalStateException("Order is not pending");
        order.setStatus("CANCELLED");
    }

    public List<LimitOrder> getMyOrders(Long userId) {
        return limitOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
