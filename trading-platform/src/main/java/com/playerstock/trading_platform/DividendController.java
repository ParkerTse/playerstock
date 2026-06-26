package com.playerstock.trading_platform;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dividends")
public class DividendController {
    private final DividendPayoutRepository repo;
    private final UserRepository userRepo;

    public DividendController(DividendPayoutRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    @GetMapping("/my-payouts")
    public ResponseEntity<?> myPayouts(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails principal) {
        User user = userRepo.findByUsername(principal.getUsername())
            .orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        List<Map<String, Object>> result = repo.findByUserIdOrderByPaidAtDesc(user.getId())
            .stream().map(dp -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",               dp.getId());
                m.put("playerName",       dp.getPlayer().getName());
                m.put("espnGameId",       dp.getEspnGameId());
                m.put("pts",              dp.getPoints());
                m.put("reb",              dp.getRebounds());
                m.put("ast",              dp.getAssists());
                m.put("stl",              dp.getSteals());
                m.put("blk",              dp.getBlocks());
                m.put("to",               dp.getTurnovers());
                m.put("sharesLong",       dp.getSharesLong());
                m.put("sharesShort",      dp.getSharesShort());
                m.put("dividendPerShare", dp.getDividendPerShare());
                m.put("netPayout",        dp.getNetPayout());
                m.put("paidAt",           dp.getPaidAt().toString());
                return m;
            }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
