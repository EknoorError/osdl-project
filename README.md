# ⚡ Volt Charge — Smart EV Charging Management System

> A state-of-the-art JavaFX desktop application for managing electric vehicle charging stations, real-time session monitoring, billing, and system configuration.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Modules](#modules)
- [Database Schema](#database-schema)
- [How to Run](#how-to-run)
- [Default Credentials](#default-credentials)

---

## Overview

**Volt Charge** is a full-featured EV charging station admin panel built with JavaFX and SQLite. It simulates and manages the full lifecycle of a charging session — from slot assignment and live telemetry to payment confirmation and receipt generation.

Key capabilities:
- Multi-slot live charging simulation (8 slots across zones A, B, C)
- Real-time session progress updates (every second)
- Automated state transitions: `Charging → Payment Pending → Completed`
- Itemized billing with configurable per-kWh pricing rates
- Revenue tracking and daily earnings dashboard
- Admin account management (login / signup)

---

## Tech Stack

| Component    | Technology                        |
|--------------|-----------------------------------|
| Language     | Java 17+                          |
| UI Framework | JavaFX 21.0.2                     |
| Database     | SQLite (managed via Maven)        |
| Styling      | JavaFX CSS (`src/main/resources/plugin.css`) |
| Build Tool   | Apache Maven                      |

---

## Project Structure

```
Java Project/
├── pom.xml                 # Maven configuration
├── src/
│   ├── main/
│   │   ├── java/           # Java source files
│   │   │   ├── Login.java
│   │   │   ├── Signup.java
│   │   │   ├── Dashboard.java
│   │   │   ├── Plugin.java
│   │   │   ├── ActiveSession.java
│   │   │   ├── Checkout.java
│   │   │   ├── Settings.java
│   │   │   ├── ChargingSession.java
│   │   │   ├── AboutController.java
│   │   │   └── AppLauncher.java
│   │   └── resources/      # FXML, CSS, and Images
│   │       ├── About.fxml
│   │       ├── plugin.css
│   │       ├── login-bg.png
│   │       └── signup-bg.png
├── sessions.db             # SQLite database (auto-created)
└── README.md               # This file
```

---

## Modules

### 🔐 Login (`Login.java`)
The entry point of the application (launched via `AppLauncher`). Validates admin credentials against the `users` table. On first launch, a default `admin / admin` account is created automatically.

### 📝 Signup (`Signup.java`)
Allows new station administrators to register. Stores full name, employee ID, station location and password.

### 📊 Dashboard (`Dashboard.java`)
The central hub. Shows:
- **Summary bento** — Total Slots, Active Sessions, Revenue Today
- **Live charging grid** — 8 slot cards updated every second, filterable by speed and status
- **Activity log** — Recent events (new sessions, completions)

A background daemon thread runs the simulation loop, advancing session progress by 3.5% per second and auto-transitioning completed sessions to `Payment Pending`.

### 🔌 Plug In (`Plugin.java`)
Used to start a new charging session. Allows the operator to:
1. Select an available slot (A-01 to C-02)
2. Enter the user's name or ID
3. Choose a connector protocol (Type 1, Type 2, CCS, CHAdeMO)

On submission, a new row is inserted into the `sessions` table.

### ⚡ Active Session (`ActiveSession.java`)
Displays live telemetry for a charging slot:
- Segmented charge gauge + percentage
- Time elapsed / estimated time remaining
- Current power (kW), peak speed, voltage
- Cell temperature, coolant flow, grid stability
- **Modify Target** button (boosts progress +15%)
- **Stop** button (ends session early → moves to `pending_payments`)

### 💳 Checkout (`Checkout.java`)
Final billing screen once a session reaches Payment Pending:
- Itemized cost breakdown (energy 75%, protocol 15%, network 10%)
- Cash or new payment method selection
- **Confirm & Pay** → inserts into `completed_sessions`, removes from `pending_payments`, returns to Dashboard
- **Print Receipt** → generates a `receipt_<slot>.txt` file

### ⚙️ Settings (`Settings.java`)
Configure per-kWh pricing rates for each protocol/speed combination. Also includes a **Reset Earnings** button that clears all `completed_sessions` records.

---

## Database Schema

The application uses a single SQLite file: **`sessions.db`** (auto-created on first run).

---

### Table: `sessions`
> Active charging sessions currently in progress.

| Column       | Type    | Description                              |
|--------------|---------|------------------------------------------|
| `id`         | INTEGER | Auto-increment primary key               |
| `slot`       | TEXT    | Slot identifier (e.g. `A-01`)            |
| `username`   | TEXT    | Name or ID of the EV driver              |
| `protocol`   | TEXT    | Connector protocol (e.g. `Type 2 Ultra Fast`) |
| `start_time` | TEXT    | Session start timestamp (`yyyy-MM-dd HH:mm:ss`) |

---

### Table: `pending_payments`
> Sessions that have completed charging but await payment confirmation.

| Column       | Type    | Description                                      |
|--------------|---------|--------------------------------------------------|
| `slot`       | TEXT    | Slot identifier                                  |
| `username`   | TEXT    | EV driver name or ID                             |
| `protocol`   | TEXT    | Connector protocol used                          |
| `start_time` | TEXT    | Original session start timestamp                 |
| `progress`   | REAL    | Charge progress at time of completion (0–100.0)  |

---

### Table: `completed_sessions`
> Fully paid and finalized sessions. Used to calculate daily revenue.

| Column       | Type    | Description                                  |
|--------------|---------|----------------------------------------------|
| `slot`       | TEXT    | Slot identifier                              |
| `username`   | TEXT    | EV driver name or ID                         |
| `protocol`   | TEXT    | Connector protocol used                      |
| `start_time` | TEXT    | Session start timestamp                      |
| `end_time`   | TEXT    | Payment confirmation timestamp               |
| `revenue`    | REAL    | Total amount charged in USD                  |

---

### Table: `users`
> Admin/operator accounts for application login.

| Column       | Type    | Description                          |
|--------------|---------|--------------------------------------|
| `username`   | TEXT    | Primary key — Employee ID used to log in |
| `password`   | TEXT    | Plain-text password                  |
| `fullname`   | TEXT    | Operator's full name                 |
| `employeeid` | TEXT    | Employee ID (same as username)       |
| `location`   | TEXT    | Managed station location             |

---

### Table: `settings`
> Key-value store for configurable system parameters (pricing rates).

| Column  | Type | Description                           |
|---------|------|---------------------------------------|
| `key`   | TEXT | Primary key — setting identifier      |
| `value` | TEXT | Setting value (stored as string)      |

**Known setting keys:**

| Key   | Description                          | Default |
|-------|--------------------------------------|---------|
| `t1s` | Type 1 Standard rate ($/kWh)         | `0.44`  |
| `t2s` | Type 2 Standard rate ($/kWh)         | `0.44`  |
| `t1u` | Type 1 Ultra Fast rate ($/kWh)       | `0.88`  |
| `t2u` | Type 2 Ultra Fast rate ($/kWh)       | `0.88`  |

---

## How to Run

### Prerequisites
- Java 17 or higher installed
- [Apache Maven](https://maven.apache.org/download.cgi) installed and configured

### Run the Application

```bash
mvn javafx:run
```

### Build the Project

```bash
mvn clean compile
```

---

## Default Credentials

| Field    | Value   |
|----------|---------|
| Username | `admin` |
| Password | `admin` |

> A default admin account is auto-seeded on first launch if no users exist in the database.

---

## Session Lifecycle

```
Slot Available
     │
     ▼
[Plug In] ──► sessions table
     │
     ▼
[Active Session] ── progress ticks +3.5%/sec
     │
     ▼ (progress = 100%)
[Payment Pending] ── pending_payments table
     │
     ▼
[Checkout] ── Confirm & Pay
     │
     ▼
[Completed] ── completed_sessions table
     │
     ▼
Slot Available again
```

---

*© 2026 Volt Charge Network. Power Admin v1.0*