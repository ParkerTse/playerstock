package com.playerstock.trading_platform;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // This gives us findById(), save(), etc. out of the box for Users!
    Optional<User> findByUsername(String username);
}