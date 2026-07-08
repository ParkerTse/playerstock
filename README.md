# PlayerStock — NBA Player Stock Market

A fantasy trading platform where NBA players are stocks. Buy, sell, and short player shares whose prices move on a bonding curve driven by real trades. Prices are seeded from actual ESPN stats.

> **Demo account:** `demo_trader` / `demo1234` — starts with $10,000 virtual cash.

---

## Screenshots

<!-- Add screenshots here after running the app locally -->
<!-- Tip: drag images into the GitHub editor to upload them -->

---

## Features

- **Live market** — all 30 NBA rosters imported from ESPN, prices set from real season stats
- **Bonding curve pricing** — buying pushes the price up, selling pushes it down
- **Buy / Sell / Short / Cover** — go long or short on any player
- **Limit orders** — set a target price and the order fills automatically when hit
- **Price alerts** — get notified when a player crosses a threshold
- **Performance dividends** — players pay out based on real game stats after each game night
- **Portfolio view** — track long value, short exposure, and unrealized P&L
- **Trade history** — full log of every trade and dividend payout
- **Leaderboard** — ranked by total portfolio value
- **Heatmap view** — color-coded market overview by price change

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 4, Spring Security (JWT) |
| Database | PostgreSQL |
| Frontend | Vanilla HTML/CSS/JS (single file, no build step) |
| Data | ESPN public API (rosters + stats) |
| Local DB | Docker Compose |

---

## Running Locally

### Prerequisites
- Java 17+
- Docker Desktop (for the database)

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/ParkerTse/playerstock.git
   cd playerstock/trading-platform
   ```

2. **Configure the app**
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```
   Edit `application.properties` and set:
   - `spring.datasource.url` → `jdbc:postgresql://localhost:5432/sports_stock_market`
   - `spring.datasource.username` → `market_user`
   - `spring.datasource.password` → `market_password`
   - `jwt.secret` → any random string, 32+ characters

3. **Start the database**
   ```bash
   docker compose up -d
   ```

4. **Run the app**
   ```bash
   ./mvnw spring-boot:run
   ```

5. **Open the app**
   Visit [http://localhost:8080](http://localhost:8080)

On first startup, the app automatically imports all active NBA players from ESPN and prices them based on their season stats. This takes about 30 seconds — watch the terminal for progress.

---

## Demo Account

| Username | Password | Starting Balance |
|---|---|---|
| `demo_trader` | `demo1234` | $10,000 |

Or register your own account from the login screen.

---

## Admin Endpoints

These are unauthenticated endpoints for managing the market:

| Endpoint | Description |
|---|---|
| `POST /api/admin/import-nba?reset=true` | Wipe players and re-import all NBA rosters |
| `POST /api/admin/sync-rosters` | Update team/position for existing players, add new signings |
| `POST /api/admin/reprice` | Re-price all players from latest ESPN stats |
| `POST /api/admin/reset-market` | Clear all trades/portfolios, restore balances to $10,000 |
| `POST /api/admin/process-dividends` | Manually trigger dividend processing |

---

## How Pricing Works

Player prices follow a **bonding curve**:

```
price = ipo_price + 0.1 × √(net_supply)
```

Where `net_supply = shares_bought − shares_sold`. Every buy increases the price slightly; every sell decreases it. IPO prices are derived from ESPN season averages using a fantasy-scoring formula:

```
ipo = (pts × 1.0) + (reb × 1.2) + (ast × 1.5) + (stl × 2.0) + (blk × 2.0) − (to × 1.0)
```

This means stars like LeBron and Curry start at higher prices than role players.
