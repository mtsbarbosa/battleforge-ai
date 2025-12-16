# BattleForge AI

A command-line tool for simulating hundreds of Keyforge deck battles and generating comprehensive statistical analysis.

## Overview

BattleForge AI is designed to execute large-scale simulations of Keyforge deck battles, providing deep insights into deck performance, matchup statistics, and meta analysis. The tool can simulate thousands of games between different deck configurations and output detailed analytics.

## Features

- **Mass Battle Simulation**: Execute hundreds or thousands of battles between Keyforge decks
- **Statistical Analysis**: Generate comprehensive statistics on win rates, average game length, and performance metrics
- **Deck Comparison**: Compare multiple decks against each other in round-robin or tournament formats
- **Deck Fetching**: Automatically fetch decks from Keyforge API or Decks of Keyforge
- **Customizable Parameters**: Configure simulation parameters like number of battles, timeout limits, and battle conditions
- **Export Results**: Output results in various formats (JSON, CSV, HTML reports)
- **Performance Metrics**: Track game duration, turn counts, and key performance indicators

## Prerequisites

- Java 8 or higher
- Leiningen 2.9.0 or higher
- Clojure 1.11.1

## Installation

1. Clone the repository:
```bash
git clone https://github.com/your-username/battleforge-ai.git
cd battleforge-ai
```

2. Install dependencies:
```bash
lein deps
```

3. Run tests to verify installation:
```bash
lein test
```

4. Verify CLI is working:
```bash
lein run --help
```

## Architecture

BattleForge AI follows the **Diplomat Architecture** pattern with clear layer separation:

```
┌─────────────────┐
│  CLI Diplomat   │ ← Command-line interface
│   (cli.clj)     │ ← Argument parsing, user interaction
└─────────────────┘
         │ calls
┌─────────────────┐
│   Controllers   │ ← Business logic orchestration
│   (deck.clj)    │ ← Workflow coordination
└─────────────────┘
         │ calls
┌─────────────────┐
│   Diplomats     │ ← External I/O operations
│ (file-storage)  │ ← HTTP, file system, database
└─────────────────┘
         │ calls
┌─────────────────┐
│   Adapters      │ ← Pure data transformation
│ (deck-storage)  │ ← Format conversion, validation
└─────────────────┘
         │ uses
┌─────────────────┐
│   Models        │ ← Domain entities and schemas
│   (deck.clj)    │
└─────────────────┘
```

**Key Principles:**
- **CLI Diplomat** handles external interface and calls **Controllers**
- **Controllers** orchestrate business logic and call **Diplomats**
- **Diplomats** handle all I/O operations and call **Adapters**
- **Adapters** are pure functions with no side effects
- **Models** define domain entities with schema validation

## Quick Start

### Fetch Decks from Online Sources

```bash
# Fetch a deck from official Keyforge API by UUID
lein run fetch-deck --source keyforge 550e8400-e29b-41d4-a716-446655440000

# For enhanced SAS ratings, set DoK API key first:
export DOK_API_KEY="your-api-key-here"

# Fetch from Decks of Keyforge by search (with SAS ratings)
lein run fetch-deck --source search --name "Archimedes the Wise"

# Fetch from Decks of Keyforge by UUID (with SAS ratings)
lein run fetch-deck --source dok 550e8400-e29b-41d4-a716-446655440000

# Save to custom directory
lein run fetch-deck --source keyforge --output-dir ./my-decks/ 550e8400-e29b-41d4-a716-446655440000
```

### Basic Battle Simulation
```bash
# Simulate 100 battles between two decks
lein run battle --deck1 path/to/deck1.json --deck2 path/to/deck2.json --battles 100

# Run with custom parameters
lein run battle --deck1 deck1.json --deck2 deck2.json --battles 500 --timeout 30 --output results.json
```

### Mass Simulation
```bash
# Run tournament-style simulation with multiple decks
lein run simulate --decks-dir ./decks/ --format tournament --battles-per-match 50

# Round-robin format with all deck combinations
lein run simulate --decks-dir ./decks/ --format round-robin --battles-per-match 100
```

### Statistics Analysis
```bash
# Generate statistics from previous simulation results
lein run stats --input results.json --output-format html --output stats-report.html
```

## Project Structure

```
src/
├── battleforge_ai/
│   ├── core.clj              # Main entry point and CLI
│   ├── models/               # Data models and schemas
│   │   ├── deck.clj         # Deck representation
│   │   ├── game.clj         # Game state models
│   │   └── battle.clj       # Battle result models
│   ├── logic/               # Pure business logic
│   │   ├── game_engine.clj  # Core game simulation logic
│   │   ├── deck_analyzer.clj # Deck analysis functions
│   │   └── matchmaker.clj   # Battle pairing logic
│   ├── simulation/          # Simulation orchestration
│   │   ├── runner.clj       # Simulation execution
│   │   ├── config.clj       # Configuration management
│   │   └── batch.clj        # Batch processing
│   ├── analysis/            # Statistical analysis
│   │   ├── stats.clj        # Statistical calculations
│   │   ├── reporting.clj    # Report generation
│   │   └── export.clj       # Data export utilities
│   ├── adapters/            # Data transformation (pure functions)
│   │   ├── keyforge_api.clj         # Keyforge API response transformation
│   │   ├── decks_of_keyforge_api.clj # DoK API response transformation
│   │   └── deck_storage.clj         # Deck serialization/deserialization
│   └── diplomat/            # External I/O operations (calls adapters)
│       ├── cli.clj              # Command-line interface
│       ├── file_storage.clj     # File system operations
│       └── http_out/            # HTTP client diplomats
│           ├── keyforge_api.clj     # Keyforge API HTTP requests
│           └── decks_of_keyforge_api.clj # DoK API HTTP requests
```

## Usage Examples

### Deck Management

#### Fetch Decks by UUID
```bash
# From official Keyforge API (requires valid deck UUID)
lein run fetch-deck --source keyforge 12345678-1234-1234-1234-123456789012

# From Decks of Keyforge (works with Keyforge UUID or DoK ID)
lein run fetch-deck --source dok 12345678-1234-1234-1234-123456789012
```

#### Search and Fetch Decks
```bash
# Search by deck name on Decks of Keyforge
lein run fetch-deck --source search --name "Quantum Thief Assembly"

# Search with partial name
lein run fetch-deck --source search --name "Archimedes"
```

#### Deck Management Options
```bash
# Save to specific directory
lein run fetch-deck --source keyforge --output-dir ./tournament-decks/ UUID

# Overwrite existing deck files
lein run fetch-deck --source search --name "Deck Name" --overwrite

# Fetch multiple decks for tournament
mkdir ./tournament-decks
lein run fetch-deck --source keyforge --output-dir ./tournament-decks/ UUID1
lein run fetch-deck --source keyforge --output-dir ./tournament-decks/ UUID2
lein run fetch-deck --source keyforge --output-dir ./tournament-decks/ UUID3
```

### Single Deck vs Deck Battle
```bash
lein run battle \
  --deck1 "./decks/mars-shadows-logos.json" \
  --deck2 "./decks/brobnar-dis-sanctum.json" \
  --battles 1000 \
  --output "./results/mars-vs-brobnar.json"
```

### Tournament Simulation
```bash
lein run simulate \
  --decks-dir "./tournament-decks/" \
  --format tournament \
  --battles-per-match 100 \
  --rounds 3 \
  --output "./results/tournament-results.json"
```

### Custom Analysis
```bash
lein run stats \
  --input "./results/tournament-results.json" \
  --metrics "win-rate,avg-turns,house-performance" \
  --output-format csv \
  --output "./analysis/detailed-stats.csv"
```

## Configuration

Create a `config.edn` file to customize default behavior:

```clojure
{:simulation {:default-battles 100
              :timeout-minutes 30
              :parallel-workers 4}
 :output {:default-format :json
          :include-detailed-logs false
          :results-directory "./results"
          :backup-results? true}
 :analysis {:metrics [:win-rate :avg-game-length :turn-count]
            :confidence-interval 0.95}
 :keyforge {:api-base-url "https://www.keyforgegame.com/api"
            :rate-limit-ms 1000
            :cache-decks? true
            :cache-directory "./cache/decks"}
 :decks-of-keyforge {:api-base-url "https://decksofkeyforge.com/public-api"
                     :api-version "v3"
                     :api-key nil ; Set your DoK API key here
                     :rate-limit-ms 1000
                     :timeout-ms 30000}}
```

### Decks of Keyforge API v3 Support

BattleForge AI now supports the **new Decks of Keyforge v3 API** which provides enhanced SAS ratings and deck statistics:

#### Getting an API Key
1. **Contact DoK**: Retrieve it once logged in [here](https://decksofkeyforge.com/about/sellers-and-devs)
2. **Set Your API Key** (choose one method):

   **Option A: Environment Variable (Recommended for Security)**
   ```bash
   # Set environment variable (replace with your actual key)
   export DOK_API_KEY="your-api-key-here"
   
   # For permanent setup, add to your shell profile:
   echo 'export DOK_API_KEY="your-api-key-here"' >> ~/.bashrc
   # or for zsh:
   echo 'export DOK_API_KEY="your-api-key-here"' >> ~/.zshrc
   ```

   **Option B: Configuration File**
   ```clojure
   ;; In config.edn
   :decks-of-keyforge {:api-key "your-api-key-here"}
   ```
   
   ⚠️ **Security Note**: Environment variables are preferred as they keep API keys out of version control.

#### Benefits with API Key
- **Complete SAS Ratings**: Access to all SAS components (Amber, Expected Amber, Artifact Control, Creature Control, Efficiency, Recursion, etc.)
- **AERC Metrics**: Detailed Amber/Expected Amber/Recursion/Creature Control analysis
- **Enhanced Statistics**: Win/loss records, usage counts, and community data
- **Better Reliability**: Authenticated access with higher rate limits

#### Fallback Behavior
Without an API key, the system automatically falls back to:
- **Official Keyforge API**: Always available but without SAS ratings
- **Full Compatibility**: All deck fetching still works, just without enhanced metrics

**Example Usage:**
```bash
# Set API key via environment variable
export DOK_API_KEY="your-api-key-here"

# Fetch with SAS ratings (requires API key)
lein run fetch-deck --source dok 938ded7f-4ea0-4698-b47c-2cdabb44e76c

# Always works - no SAS ratings needed
lein run fetch-deck --source keyforge 938ded7f-4ea0-4698-b47c-2cdabb44e76c
```

#### API Key Troubleshooting

**Check if your API key is loaded:**
```bash
# Look for this log message when running any DoK command:
# "Loaded DoK API key from DOK_API_KEY environment variable"
# or "Loaded DoK API key from configuration file"
```

**Common issues:**
- **No API key message**: Set `DOK_API_KEY` environment variable or add to `config.edn`
- **Authentication errors**: Verify your API key is correct
- **Still no SAS ratings**: Some decks may not have SAS data calculated yet

**Verify environment variable:**
```bash
echo $DOK_API_KEY  # Should show your API key
```

## CLI Options

### Global Options
- `--help, -h`: Show help information
- `--verbose, -v`: Enable verbose logging
- `--config, -c`: Specify custom config file

### Fetch-Deck Command
- `--source, -s`: Source to fetch from (keyforge, dok, search)
- `--output-dir, -o`: Output directory for deck file (default: ./decks)
- `--name, -n`: Deck name (required for search source)
- `--overwrite`: Overwrite existing deck file

### Battle Command
- `--deck1, -1`: Path to first deck file
- `--deck2, -2`: Path to second deck file
- `--battles, -b`: Number of battles to simulate (default: 100)
- `--timeout, -t`: Timeout per game in minutes (default: 30)
- `--output, -o`: Output file path

### Simulate Command
- `--decks-dir, -d`: Directory containing deck files
- `--format, -f`: Simulation format (tournament, round-robin, custom)
- `--battles-per-match, -b`: Battles per matchup
- `--parallel, -p`: Number of parallel workers

### Stats Command
- `--input, -i`: Input results file
- `--output-format`: Output format (json, csv, html)
- `--metrics, -m`: Specific metrics to analyze

## Data Sources

### Official Keyforge API
- **Source**: `keyforge`
- **Requirements**: Valid 36-character UUID from keyforgegame.com
- **Features**: Official deck data, always up-to-date
- **Rate Limit**: 1 request per second

### Decks of Keyforge
- **Source**: `dok` or `search`
- **Requirements**: Keyforge UUID, DoK ID, or deck name
- **Features**: Additional statistics, search capability, community data
- **Rate Limit**: 1 request per second

### URL Extraction
You can extract UUIDs from Keyforge URLs:
```bash
# From: https://www.keyforgegame.com/deck-details/12345678-1234-1234-1234-123456789012
lein run fetch-deck --source keyforge 12345678-1234-1234-1234-123456789012
```

## Development

### Running Tests
```bash
# Run all tests
lein test

# Run specific namespace tests
lein test battleforge-ai.adapters.deck-storage-test

# Run with coverage
lein test :coverage
```

### Linting
```bash
# Run all linters (clj-kondo + eastwood)
lein lint

# Run clj-kondo only
lein lint:kondo

# Run eastwood only
lein lint:eastwood
```

### Code Formatting (zprint)
```bash
# Format all Clojure files
make fmt

# Check formatting without modifying (CI-friendly)
make fmt-check

# Run all checks (format + lint + test)
make all
```

### Development REPL
```bash
lein repl
```

### Building
```bash
# Create standalone JAR
lein uberjar

# Run the JAR directly
java -jar target/uberjar/battleforge-ai-0.1.0-SNAPSHOT-standalone.jar --help
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes following the Clojure style guide
4. Add tests for new functionality
5. Ensure all tests pass (`lein test`)
6. Commit your changes (`git commit -am 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built for the Keyforge community
- Inspired by competitive deck analysis needs
- Uses functional programming principles for reliable simulations
- Integrates with official Keyforge API and community tools
