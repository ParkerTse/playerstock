package com.playerstock.trading_platform;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final PlayerRepository playerRepository;
    private final PortfolioRepository portfolioRepository;
    private final NbaImportService nbaImportService;
    private final UserRepository userRepository;
    private final TradeLogRepository tradeLogRepository;
    private final LimitOrderRepository limitOrderRepository;
    private final DividendPayoutRepository dividendPayoutRepository;
    private final ProcessedGameRepository processedGameRepository;
    private final DividendService dividendService;

    public AdminController(PlayerRepository playerRepository,
                           PortfolioRepository portfolioRepository,
                           NbaImportService nbaImportService,
                           UserRepository userRepository,
                           TradeLogRepository tradeLogRepository,
                           LimitOrderRepository limitOrderRepository,
                           DividendPayoutRepository dividendPayoutRepository,
                           ProcessedGameRepository processedGameRepository,
                           DividendService dividendService) {
        this.playerRepository        = playerRepository;
        this.portfolioRepository     = portfolioRepository;
        this.nbaImportService        = nbaImportService;
        this.userRepository          = userRepository;
        this.tradeLogRepository      = tradeLogRepository;
        this.limitOrderRepository    = limitOrderRepository;
        this.dividendPayoutRepository = dividendPayoutRepository;
        this.processedGameRepository  = processedGameRepository;
        this.dividendService          = dividendService;
    }

    // Imports NBA players from BallDontLie.
    // ?reset=true  — wipes existing players and portfolios first (full re-import)
    // ?reset=false — skips players already in DB, continues where a previous run left off (default)
    @PostMapping("/import-nba")
    public ResponseEntity<String> importNba(@RequestParam(defaultValue = "false") boolean reset) {
        if (reset) {
            portfolioRepository.deleteAll();
            playerRepository.deleteAll();
        }
        nbaImportService.importActivePlayers();
        long existing = playerRepository.count();
        return ResponseEntity.ok(
            (reset ? "Full reset + " : "Continuing from ") +
            existing + " existing players. Check server logs for progress."
        );
    }

    // Updates team/position for existing players and adds new signings — does not touch portfolios or prices
    @PostMapping("/sync-rosters")
    public ResponseEntity<String> syncRosters() {
        nbaImportService.syncRosters();
        return ResponseEntity.ok("Roster sync started. Check server logs for progress.");
    }

    // Re-runs stat enrichment for all players using latest ESPN data — does not wipe portfolios
    @PostMapping("/reprice")
    public ResponseEntity<String> reprice() {
        Thread t = new Thread(() -> nbaImportService.repriceAllPlayers());
        t.setDaemon(true);
        t.start();
        return ResponseEntity.ok("Reprice started for all players. Check server logs for progress.");
    }

    // Wipes all portfolios, trade logs, limit orders, dividends, resets outstanding shares to 0
    // and restores every user balance to $10,000. Use after changing the bonding curve scale factor.
    @PostMapping("/reset-market")
    @Transactional
    public ResponseEntity<String> resetMarket() {
        dividendPayoutRepository.deleteAll();
        processedGameRepository.deleteAll();
        limitOrderRepository.deleteAll();
        tradeLogRepository.deleteAll();
        portfolioRepository.deleteAll();

        List<com.playerstock.trading_platform.Player> players = playerRepository.findAll();
        for (com.playerstock.trading_platform.Player p : players) {
            p.setOutstandingShares(0);
        }
        playerRepository.saveAll(players);

        List<com.playerstock.trading_platform.User> users = userRepository.findAll();
        for (com.playerstock.trading_platform.User u : users) {
            u.setBalance(new BigDecimal("10000.00"));
        }
        userRepository.saveAll(users);

        return ResponseEntity.ok("Market reset complete. All positions cleared, balances restored to $10,000.");
    }

    // Manually triggers dividend processing for today and yesterday
    @PostMapping("/process-dividends")
    public ResponseEntity<String> processDividends(
            @RequestParam(required = false) String date) {
        Thread t = new Thread(() -> {
            if (date != null && !date.isBlank()) {
                dividendService.processDateManual(LocalDate.parse(date));
            } else {
                dividendService.processRecentGames();
            }
        });
        t.setDaemon(true);
        t.start();
        return ResponseEntity.ok("Dividend processing triggered. Check server logs.");
    }
}
