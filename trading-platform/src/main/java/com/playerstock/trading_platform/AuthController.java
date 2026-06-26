package com.playerstock.trading_platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    // 1. REGISTER A NEW USER ACCOUNT
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken.");
        }

        User user = new User(username, encoder.encode(password), new BigDecimal("10000.00"));
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    // 2. LOGIN & RETURN THE SECURE TOKEN
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            return ResponseEntity.badRequest().body("No account found with that username.");
        }

        User user = found.get();
        if (!encoder.matches(password, user.getPassword())) {
            return ResponseEntity.badRequest().body("Incorrect password.");
        }

        String token = jwtUtils.generateJwtToken(username);

        return ResponseEntity.ok(Map.of(
            "token",    token,
            "username", user.getUsername(),
            "userId",   String.valueOf(user.getId())
        ));
    }
}