package com.playerstock.trading_platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DividendService {
    private static final Logger log = LoggerFactory.getLogger(DividendService.class);
    private static final String SCOREBOARD_URL =
        "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/scoreboard?dates=";
    private static final String SUMMARY_URL =
        "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/summary?event=";

    private final PlayerRepository         playerRepo;
    private final PortfolioRepository      portfolioRepo;
    private final ProcessedGameRepository  processedGameRepo;
    private final DividendPayoutRepository dividendPayoutRepo;
    private final UserRepository           userRepo;
    private final HttpClient               http = HttpClient.newHttpClient();
    private final ObjectMapper             mapper = new ObjectMapper();

    public DividendService(PlayerRepository playerRepo,
                           PortfolioRepository portfolioRepo,
                           ProcessedGameRepository processedGameRepo,
                           DividendPayoutRepository dividendPayoutRepo,
                           UserRepository userRepo) {
        this.playerRepo         = playerRepo;
        this.portfolioRepo      = portfolioRepo;
        this.processedGameRepo  = processedGameRepo;
        this.dividendPayoutRepo = dividendPayoutRepo;
        this.userRepo           = userRepo;
    }

    @Scheduled(fixedDelay = 1_800_000) // every 30 minutes
    @Transactional
    public void processRecentGames() {
        LocalDate today     = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        checkDate(yesterday);
        checkDate(today);
    }

    @Transactional
    public void processDateManual(LocalDate date) {
        checkDate(date);
    }

    private void checkDate(LocalDate date) {
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        try {
            String body = get(SCOREBOARD_URL + dateStr);
            JsonNode root = mapper.readTree(body);
            JsonNode events = root.path("events");
            if (!events.isArray()) return;
            for (JsonNode event : events) {
                String status = event.path("status").path("type").path("name").asText("");
                if (!"STATUS_FINAL".equals(status)) continue;
                String gameId = event.path("id").asText("");
                if (gameId.isEmpty() || processedGameRepo.existsById(gameId)) continue;
                processGame(gameId);
                processedGameRepo.save(new ProcessedGame(gameId));
            }
        } catch (Exception e) {
            log.error("Error checking scoreboard for {}: {}", dateStr, e.getMessage());
        }
    }

    private void processGame(String gameId) throws IOException, InterruptedException {
        String body = get(SUMMARY_URL + gameId);
        JsonNode root = mapper.readTree(body);
        JsonNode boxscorePlayers = root.path("boxscore").path("players");
        if (!boxscorePlayers.isArray()) return;

        for (JsonNode teamSection : boxscorePlayers) {
            JsonNode statsNode = teamSection.path("statistics");
            if (!statsNode.isArray() || statsNode.isEmpty()) continue;
            JsonNode firstStat = statsNode.get(0);
            JsonNode names  = firstStat.path("names");
            JsonNode athletes = firstStat.path("athletes");
            if (!names.isArray() || !athletes.isArray()) continue;

            int ptsIdx = -1, rebIdx = -1, astIdx = -1, stlIdx = -1, blkIdx = -1, toIdx = -1;
            for (int i = 0; i < names.size(); i++) {
                switch (names.get(i).asText("")) {
                    case "PTS" -> ptsIdx = i;
                    case "REB" -> rebIdx = i;
                    case "AST" -> astIdx = i;
                    case "STL" -> stlIdx = i;
                    case "BLK" -> blkIdx = i;
                    case "TO"  -> toIdx  = i;
                }
            }
            if (ptsIdx < 0) continue;

            for (JsonNode athlete : athletes) {
                if (athlete.path("didNotPlay").asBoolean(false)) continue;
                String playerName = athlete.path("athlete").path("displayName").asText("");
                if (playerName.isEmpty()) continue;

                JsonNode stats = athlete.path("stats");
                if (!stats.isArray()) continue;

                double pts = safeDouble(stats, ptsIdx);
                double reb = safeDouble(stats, rebIdx);
                double ast = safeDouble(stats, astIdx);
                double stl = safeDouble(stats, stlIdx);
                double blk = safeDouble(stats, blkIdx);
                double to  = safeDouble(stats, toIdx);

                double dividendPerShare = Math.max(0,
                    Math.min(10.0, pts * 0.07 + reb * 0.05 + ast * 0.09
                                 + stl * 0.12 + blk * 0.12 - to * 0.08));

                if (dividendPerShare == 0) continue;

                playerRepo.findByName(playerName).ifPresentOrElse(
                    player -> payDividends(player, gameId, pts, reb, ast, stl, blk, to, dividendPerShare),
                    () -> log.debug("No player found for ESPN name: {}", playerName)
                );
            }
        }
    }

    private void payDividends(Player player, String gameId,
                               double pts, double reb, double ast,
                               double stl, double blk, double to,
                               double dividendPerShare) {
        List<PortfolioItem> holders = portfolioRepo.findByPlayerId(player.getId());
        if (holders.isEmpty()) return;

        List<DividendPayout> payouts = new ArrayList<>();
        for (PortfolioItem item : holders) {
            int sharesLong  = Math.max(0, item.getSharesOwned());
            int sharesShort = Math.max(0, item.getSharesShorted());
            if (sharesLong == 0 && sharesShort == 0) continue;

            double netPayout = dividendPerShare * sharesLong
                             - dividendPerShare * sharesShort;

            User user = item.getUser();
            user.setBalance(user.getBalance().add(BigDecimal.valueOf(netPayout)));
            userRepo.save(user);

            DividendPayout dp = new DividendPayout();
            dp.setUser(user);
            dp.setPlayer(player);
            dp.setEspnGameId(gameId);
            dp.setPoints(pts);
            dp.setRebounds(reb);
            dp.setAssists(ast);
            dp.setSteals(stl);
            dp.setBlocks(blk);
            dp.setTurnovers(to);
            dp.setSharesLong(sharesLong);
            dp.setSharesShort(sharesShort);
            dp.setDividendPerShare(dividendPerShare);
            dp.setNetPayout(netPayout);
            payouts.add(dp);
        }
        dividendPayoutRepo.saveAll(payouts);
        log.info("Paid dividends for {} in game {}: ${}/share, {} holders",
            player.getName(), gameId, String.format("%.2f", dividendPerShare), payouts.size());
    }

    private double safeDouble(JsonNode arr, int idx) {
        if (idx < 0 || idx >= arr.size()) return 0;
        try { return Double.parseDouble(arr.get(idx).asText("0")); }
        catch (NumberFormatException e) { return 0; }
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0")
            .GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }
}
