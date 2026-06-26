package com.playerstock.trading_platform;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository      userRepository;
    private final PortfolioRepository portfolioRepository;

    public UserController(UserRepository userRepository, PortfolioRepository portfolioRepository) {
        this.userRepository      = userRepository;
        this.portfolioRepository = portfolioRepository;
    }

    @GetMapping("/me")
    public User getCurrentUser(@AuthenticationPrincipal User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    @GetMapping("/leaderboard")
    public List<Map<String, Object>> getLeaderboard() {
        List<User>          users = userRepository.findAll();
        List<PortfolioItem> items = portfolioRepository.findAll();

        // Group portfolio items by userId
        Map<Long, List<PortfolioItem>> byUser = new HashMap<>();
        for (PortfolioItem item : items) {
            byUser.computeIfAbsent(item.getUser().getId(), k -> new ArrayList<>()).add(item);
        }

        List<Map<String, Object>> board = new ArrayList<>();
        for (User user : users) {
            List<PortfolioItem> portfolio = byUser.getOrDefault(user.getId(), List.of());
            double longValue     = portfolio.stream().mapToDouble(i -> i.getSharesOwned()   * i.getPlayer().getCurrentPrice()).sum();
            double shortExposure = portfolio.stream().mapToDouble(i -> i.getSharesShorted() * i.getPlayer().getCurrentPrice()).sum();
            double totalValue    = user.getBalance().doubleValue() + longValue - shortExposure;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId",        user.getId());
            entry.put("username",      user.getUsername());
            entry.put("cash",          user.getBalance().doubleValue());
            entry.put("longValue",     longValue);
            entry.put("shortExposure", shortExposure);
            entry.put("totalValue",    totalValue);
            board.add(entry);
        }

        board.sort((a, b) -> Double.compare((double) b.get("totalValue"), (double) a.get("totalValue")));
        return board;
    }
}
