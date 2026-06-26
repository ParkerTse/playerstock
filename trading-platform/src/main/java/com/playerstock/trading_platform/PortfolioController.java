package com.playerstock.trading_platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/portfolios")
@CrossOrigin(origins = "*")
public class PortfolioController {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @GetMapping("/me")
    public List<PortfolioItem> getMyPortfolio(@AuthenticationPrincipal User user) {
        return portfolioRepository.findByUserId(user.getId());
    }
}