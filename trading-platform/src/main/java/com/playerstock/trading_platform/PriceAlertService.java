package com.playerstock.trading_platform;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserRepository       userRepository;
    private final PlayerRepository     playerRepository;

    public PriceAlertService(PriceAlertRepository priceAlertRepository,
                             UserRepository userRepository,
                             PlayerRepository playerRepository) {
        this.priceAlertRepository = priceAlertRepository;
        this.userRepository       = userRepository;
        this.playerRepository     = playerRepository;
    }

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void checkAlerts() {
        List<PriceAlert> active = priceAlertRepository.findByTriggeredFalse();
        for (PriceAlert alert : active) {
            double  price     = alert.getPlayer().getCurrentPrice();
            boolean triggered = "ABOVE".equals(alert.getDirection())
                ? price >= alert.getTargetPrice()
                : price <= alert.getTargetPrice();
            if (triggered) {
                alert.setTriggered(true);
                alert.setTriggeredAt(LocalDateTime.now());
            }
        }
    }

    @Transactional
    public PriceAlert createAlert(Long userId, Long playerId,
                                  String direction, double targetPrice) {
        if (targetPrice <= 0) throw new IllegalArgumentException("Target price must be positive");
        if (!"ABOVE".equals(direction) && !"BELOW".equals(direction))
            throw new IllegalArgumentException("Direction must be ABOVE or BELOW");

        User   user   = userRepository.findById(userId).orElseThrow();
        Player player = playerRepository.findById(playerId).orElseThrow();

        PriceAlert alert = new PriceAlert();
        alert.setUser(user);
        alert.setPlayer(player);
        alert.setDirection(direction);
        alert.setTargetPrice(targetPrice);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void dismissAlert(Long userId, Long alertId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        if (!alert.getUser().getId().equals(userId))
            throw new IllegalArgumentException("Not your alert");
        alert.setDismissed(true);
    }

    public List<PriceAlert> getMyAlerts(Long userId) {
        return priceAlertRepository.findByUserIdAndDismissedFalseOrderByCreatedAtDesc(userId);
    }
}
