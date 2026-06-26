package com.playerstock.trading_platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NbaImportService {

    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public NbaImportService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    private static final String ESPN_TEAMS_URL   = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams";
    private static final String ESPN_ROSTER_URL  = "https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams/%s/roster";

    public void importActivePlayers() {
        System.out.println("Fetching active NBA rosters from ESPN...");
        try {
            // Step 1: get all 30 current NBA team IDs and names
            String teamsJson = restClient.get().uri(ESPN_TEAMS_URL).retrieve().body(String.class);
            JsonNode teams   = objectMapper.readTree(teamsJson)
                .path("sports").get(0).path("leagues").get(0).path("teams");

            List<Player> toSave   = new ArrayList<>();
            int          teamsDone = 0;

            // Step 2: fetch the active roster for each team
            for (JsonNode entry : teams) {
                JsonNode team     = entry.get("team");
                String   teamId   = team.get("id").asText();
                String   teamName = team.get("displayName").asText();

                String rosterJson = restClient.get()
                    .uri(String.format(ESPN_ROSTER_URL, teamId))
                    .retrieve()
                    .body(String.class);

                JsonNode athletes = objectMapper.readTree(rosterJson).path("athletes");
                for (JsonNode athlete : athletes) {
                    String name     = athlete.path("displayName").asText("").trim();
                    String position = athlete.path("position").path("abbreviation").asText("");
                    String headshot = athlete.path("headshot").path("href").asText("");

                    if (!name.isBlank() && !playerRepository.existsByName(name)) {
                        Player p = new Player(name, position, teamName);
                        if (!headshot.isBlank()) p.setImageUrl(headshot);
                        toSave.add(p);
                    }
                }

                System.out.printf("  %-30s — %d players so far%n", teamName, toSave.size());
                teamsDone++;
                if (teamsDone < teams.size()) Thread.sleep(100); // polite pacing
            }

            playerRepository.saveAll(toSave);
            System.out.printf("Import complete — %d active NBA players saved across 30 teams.%n", toSave.size());

            if (!toSave.isEmpty()) {
                Thread enricher = new Thread(() -> enrichWithStats(toSave));
                enricher.setDaemon(true);
                enricher.setName("enricher");
                enricher.start();
            }

        } catch (Exception e) {
            System.out.println("ESPN import failed: " + e.getMessage());
        }
    }


    // Re-runs stat enrichment for ALL existing players (season reprice, no data wipe)
    public void repriceAllPlayers() {
        List<Player> all = playerRepository.findAll();
        System.out.printf("Repricing %d players from latest ESPN stats...%n", all.size());
        enrichWithStats(all);
    }

    // Runs daily at 3 AM — updates team/position for existing players, adds new signings/rookies
    @Scheduled(cron = "0 0 3 * * *")
    public void syncRosters() {
        System.out.println("Daily roster sync starting...");
        try {
            // Load all existing players into a name→player map for O(1) lookup
            Map<String, Player> existing = new HashMap<>();
            playerRepository.findAll().forEach(p -> existing.put(p.getName(), p));

            String teamsJson = restClient.get().uri(ESPN_TEAMS_URL).retrieve().body(String.class);
            JsonNode teams   = objectMapper.readTree(teamsJson)
                .path("sports").get(0).path("leagues").get(0).path("teams");

            List<Player> toUpdate = new ArrayList<>();
            List<Player> toAdd    = new ArrayList<>();
            int teamsDone = 0;

            for (JsonNode entry : teams) {
                JsonNode team     = entry.get("team");
                String   teamId   = team.get("id").asText();
                String   teamName = team.get("displayName").asText();

                String rosterJson = restClient.get()
                    .uri(String.format(ESPN_ROSTER_URL, teamId))
                    .retrieve().body(String.class);

                JsonNode athletes = objectMapper.readTree(rosterJson).path("athletes");
                for (JsonNode athlete : athletes) {
                    String name     = athlete.path("displayName").asText("").trim();
                    String position = athlete.path("position").path("abbreviation").asText("");
                    String headshot = athlete.path("headshot").path("href").asText("");

                    if (name.isBlank()) continue;

                    Player p = existing.get(name);
                    if (p != null) {
                        boolean changed = false;
                        if (!teamName.equals(p.getTeam()))    { p.setTeam(teamName);    changed = true; }
                        if (!position.equals(p.getPosition())) { p.setPosition(position); changed = true; }
                        if (changed) toUpdate.add(p);
                    } else {
                        Player newPlayer = new Player(name, position, teamName);
                        if (!headshot.isBlank()) newPlayer.setImageUrl(headshot);
                        toAdd.add(newPlayer);
                    }
                }

                teamsDone++;
                if (teamsDone < teams.size()) Thread.sleep(100);
            }

            if (!toUpdate.isEmpty()) {
                playerRepository.saveAll(toUpdate);
                System.out.printf("  Updated team/position for %d existing players.%n", toUpdate.size());
            }

            if (!toAdd.isEmpty()) {
                playerRepository.saveAll(toAdd);
                System.out.printf("  Added %d new players (rookies/new signings).%n", toAdd.size());
                Thread enricher = new Thread(() -> enrichWithStats(toAdd));
                enricher.setDaemon(true);
                enricher.setName("enricher");
                enricher.start();
            }

            if (toUpdate.isEmpty() && toAdd.isEmpty()) {
                System.out.println("  No roster changes detected.");
            }

        } catch (Exception e) {
            System.out.println("Roster sync failed: " + e.getMessage());
        }
    }

    private static final String ESPN_STATS_BASE =
        "https://site.web.api.espn.com/apis/common/v3/sports/basketball/nba/statistics/byathlete" +
        "?seasontype=2&limit=600&season=";

    // Each entry: [gamesPlayed, pts, reb, ast, stl, blk, to]
    private Map<String, double[]> fetchSeasonStats(int season) {
        Map<String, double[]> result = new HashMap<>();
        try {
            String json = restClient.get().uri(ESPN_STATS_BASE + season).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(json);

            Map<String, Map<String, Integer>> catIndex = new HashMap<>();
            for (JsonNode cat : root.path("categories")) {
                String catName = cat.path("name").asText();
                Map<String, Integer> nameToIdx = new HashMap<>();
                JsonNode names = cat.path("names");
                for (int i = 0; i < names.size(); i++) {
                    nameToIdx.put(names.get(i).asText(), i);
                }
                catIndex.put(catName, nameToIdx);
            }

            int gpIdx  = catIndex.getOrDefault("general",   Map.of()).getOrDefault("gamesPlayed",  -1);
            int rebIdx = catIndex.getOrDefault("general",   Map.of()).getOrDefault("avgRebounds",   -1);
            int ptsIdx = catIndex.getOrDefault("offensive", Map.of()).getOrDefault("avgPoints",     -1);
            int astIdx = catIndex.getOrDefault("offensive", Map.of()).getOrDefault("avgAssists",    -1);
            int toIdx  = catIndex.getOrDefault("offensive", Map.of()).getOrDefault("avgTurnovers",  -1);
            int stlIdx = catIndex.getOrDefault("defensive", Map.of()).getOrDefault("avgSteals",     -1);
            int blkIdx = catIndex.getOrDefault("defensive", Map.of()).getOrDefault("avgBlocks",     -1);

            for (JsonNode entry : root.path("athletes")) {
                String name = entry.path("athlete").path("displayName").asText("").trim();
                Map<String, JsonNode> cats = new HashMap<>();
                for (JsonNode cat : entry.path("categories")) {
                    cats.put(cat.path("name").asText(), cat.path("values"));
                }
                JsonNode g = cats.get("general"), off = cats.get("offensive"), def = cats.get("defensive");
                if (g == null || off == null || def == null) continue;

                result.put(name, new double[]{
                    gpIdx  >= 0 ? g.path(gpIdx).asDouble(0)     : 0,
                    ptsIdx >= 0 ? off.path(ptsIdx).asDouble(0)  : 0,
                    rebIdx >= 0 ? g.path(rebIdx).asDouble(0)    : 0,
                    astIdx >= 0 ? off.path(astIdx).asDouble(0)  : 0,
                    stlIdx >= 0 ? def.path(stlIdx).asDouble(0)  : 0,
                    blkIdx >= 0 ? def.path(blkIdx).asDouble(0)  : 0,
                    toIdx  >= 0 ? off.path(toIdx).asDouble(0)   : 0,
                });
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch season " + season + " stats: " + e.getMessage());
        }
        return result;
    }

    private void enrichWithStats(List<Player> players) {
        System.out.println("Fetching stats from ESPN (2025-26 primary, 2024-25 fallback by games played)...");

        Map<String, double[]> stats2026 = fetchSeasonStats(2026);
        Map<String, double[]> stats2025 = fetchSeasonStats(2025);

        int priced = 0;
        for (Player player : players) {
            double[] s26 = stats2026.get(player.getName());
            double[] s25 = stats2025.get(player.getName());

            // Pick whichever season the player played more games in
            double[] stats = null;
            if (s26 != null && s25 != null) {
                stats = s26[0] >= s25[0] ? s26 : s25;
            } else if (s26 != null) {
                stats = s26;
            } else if (s25 != null) {
                stats = s25;
            }

            if (stats == null) continue;

            double gp = stats[0], pts = stats[1], reb = stats[2],
                   ast = stats[3], stl = stats[4], blk = stats[5], to = stats[6];

            double ipo = (pts * 1.0) + (reb * 1.2) + (ast * 1.5) + (stl * 2.0) + (blk * 2.0) - (to * 1.0);
            ipo = Math.max(5.0, Math.round(ipo * 100.0) / 100.0);

            player.setIpoPrice(ipo);
            playerRepository.save(player);
            priced++;
        }

        long stillDefault = players.stream().filter(p -> p.getIpoPrice() == 10.00).count();
        System.out.printf("Stat enrichment done: %d / %d players priced. %d remain at $10 default.%n",
            priced, players.size(), stillDefault);
    }

    private void enrichWithImages(List<Player> players) {
        System.out.printf("TheSportsDB image enrichment starting for %d players...%n", players.size());
        int enriched = 0;

        for (Player player : players) {
            try {
                URI uri = UriComponentsBuilder
                    .fromUriString("https://www.thesportsdb.com/api/v1/json/3/searchplayers.php")
                    .queryParam("p", player.getName())
                    .build()
                    .toUri();

                String json    = restClient.get().uri(uri).retrieve().body(String.class);
                JsonNode root  = objectMapper.readTree(json);
                JsonNode found = root.path("player");

                if (found.isArray() && found.size() > 0) {
                    String thumb = found.get(0).path("strThumb").asText("");
                    if (!thumb.isBlank()) {
                        player.setImageUrl(thumb);
                        playerRepository.save(player);
                        enriched++;
                    }
                }

                Thread.sleep(400); // TheSportsDB free tier rate limit
            } catch (Exception e) {
                System.out.println("  Could not enrich " + player.getName() + ": " + e.getMessage());
            }
        }

        System.out.printf("Image enrichment complete — %d / %d players got images.%n", enriched, players.size());
    }
}
