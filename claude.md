# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Telegram bot for MR17dom1 residential complex that provides two main services:
1. **Car plate recognition**: Look up car owner information by license plate number
2. **Receipt download**: Download utility bills (ЖКУ) for apartments and parking spaces

Built with Kotlin, uses Gradle for builds, and integrates with:
- Telegram Bot API (kotlin-telegram-bot library)
- Custom HouseKpr API for data retrieval
- Docker for deployment

## Development Commands

### Build & Run
```bash
# Build the project
./gradlew build

# Build fat JAR with all dependencies
./gradlew shadowJar

# Run tests
./gradlew test

# Run locally (requires environment variables)
./gradlew run
```

### Docker
```bash
# Build Docker image
docker build -t mr17dom1-bot .

# Run with docker-compose
docker-compose up -d

# View logs
docker-compose logs -f
```

## Environment Variables

Required environment variables (set in `.env` file):
- `TELEGRAM_BOT_TOKEN`: Telegram bot token from BotFather
- `HOUSEKPR_HOST`: HouseKpr API host URL
- `HOUSEKPR_EMAIL`: HouseKpr API login email
- `HOUSEKPR_PASSWORD`: HouseKpr API login password

## Architecture

### Main Entry Point
`Mr17dom1Bot.kt` - Initializes the bot, sets up state management, and registers all handlers.

### State Management (In-Memory)
The bot maintains two types of user state:
- `waitingForPlate: MutableSet<Long>` - Tracks users awaiting car plate input
- `receiptStates: MutableMap<Long, ReceiptState>` - Multi-step receipt download workflow (month → type → number)

**Important**: State is in-memory only; bot restart clears all user sessions.

### Handler Architecture
Handlers are registered in the dispatcher and process user messages:

1. **CarHandler** (`handlers/CarHandler.kt`)
   - Button: "🚗 Распознать номер"
   - Flow: User clicks → bot prompts for plate → user enters plate → bot queries API → displays owner info
   - Uses `waitingForPlate` set to track state

2. **ReceiptHandler** (`handlers/ReceiptHandler.kt`)
   - Button: "📄 Скачать квитанцию"
   - Flow: Multi-step process with `ReceiptState`:
     - SELECT_MONTH: User selects from available months (YYYY-MM format)
     - SELECT_TYPE: User chooses flat (🏡 Квартира) or parking (🅿️ Машиноместо)
     - SELECT_NUMBER: User enters room number (1-144)
     - Bot downloads PDF and sends via Telegram
   - Uses `sendWithRetry()` utility for reliable file sending

3. **ResetHandler** (`handlers/ResetHandler.kt`)
   - Command: `/reset`
   - Clears user state and returns to main menu

### API Layer

**HousekprApi** (`api/HousekprApi.kt`)
- Manages authentication with auto-token refresh
- Key methods:
  - `login()`: Authenticates and stores access token
  - `getOverview(carNumber)`: Retrieves car owner information
  - `getAvailableMonths()`: Lists available billing months
  - `downloadReceiptPdf(year, month, roomType, number)`: Downloads PDF receipt

**TelegramApi** (`api/TelegramApi.kt`)
- Wrapper for Telegram file operations
- Method: `sendDocument()` for sending PDF files

### Coroutines Usage
- `botScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)`
- All API calls run asynchronously in `botScope.launch { ... }`
- UI remains responsive while fetching data or files

### Utilities
- `sendWithRetry()` in `Utils.kt`: Retry mechanism for Telegram file uploads (3 attempts, 2s delay)
- Extension: `PdfFileData.toTempFile()` converts byte arrays to temporary files

## Deployment

CI/CD pipeline (`.github/workflows/deploy-to-mr17dom1.yml`):
1. On push to `master` branch:
   - Builds Docker image with Gradle
   - Pushes to DockerHub with short SHA tag
   - SSHs to Ubuntu server
   - Updates docker-compose and restarts container

## Language & Tooling
- **Language**: Kotlin 2.2.20
- **JDK**: 21
- **Build**: Gradle with Kotlin DSL
- **Key Dependencies**:
  - kotlin-telegram-bot 6.3.0
  - Jetbrains Exposed + SQLite (currently not actively used)
  - Ktor client for HTTP
  - JSoup, HtmlUnit, Selenium (for web scraping, if needed)
  - Logback for logging

## Coding Standards
- Use idiomatic Kotlin
- Prefer immutable data (`val` over `var`)
- Use data classes for DTOs
- Avoid nullable types unless necessary
- Use sealed classes for closed hierarchies
- Prefer coroutines over threads
- Follow official Kotlin style guide
- Keep functions under 50 lines
- Use meaningful names

## Error Handling
- Prefer `Result` or sealed classes over exceptions
- Do not swallow exceptions
- Log errors with context using `logger()`
- API methods return `null` on failure rather than throwing
