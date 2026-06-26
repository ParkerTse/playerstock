package com.playerstock.trading_platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final UserRepository userRepository;
    private final NbaImportService nbaImportService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PlayerRepository playerRepository,
                           UserRepository userRepository,
                           NbaImportService nbaImportService,
                           PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.userRepository   = userRepository;
        this.nbaImportService = nbaImportService;
        this.passwordEncoder  = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (playerRepository.count() == 0) {
            nbaImportService.importActivePlayers();
        } else {
            System.out.printf("Market already has %d players — skipping import.%n", playerRepository.count());
        }

        if (userRepository.count() == 0) {
            userRepository.save(new User("demo_trader", passwordEncoder.encode("demo1234"), new BigDecimal("10000.00")));
            System.out.println("Seeded demo_trader (password: demo1234)");
        }
    }
}
