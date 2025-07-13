# Advanced Minecraft Plugin

A Minecraft plugin that adds economy, inventory saving, and ender chest features with multi-server synchronization.

## Features

- **Economy System:** Player balance management, transfers, admin controls
- **Inventory Management:** Save/load inventories with custom names, backup/restore
- **Ender Chest Management:** Save/load ender chests with custom names, backup/restore
- **Multi-Server Sync:** Automatic synchronization across multiple servers via Redis
- **gRPC API:** Optional REST API for external integrations

## Requirements

- Java 21+
- Minecraft 1.21+ (Paper recommended)
- MongoDB (data storage)
- Redis (server synchronization)

## Quick Setup

### Docker (Recommended for self-hosted)
```bash
# Start databases
docker run -d --name mongodb -p 27017:27017 mongo:latest
docker run -d --name redis -p 6379:6379 redis:latest

# Or use setup scripts
./setup-databases.sh  # Linux/Mac
setup-databases.bat   # Windows
```

### Manual Installation (self-hosted)
- **MongoDB:** Install from [mongodb.com](https://www.mongodb.com/try/download/community)
- **Redis:** Install from [redis.io](https://redis.io/download)
- **Plugin:** Put JAR in `plugins/` folder

### Cloud Setup
- **MongoDB Atlas:** Free tier at [mongodb.com/atlas](https://www.mongodb.com/atlas)
- **Redis Cloud:** Free tier at [redis.com](https://redis.com/try-free/)

## Configuration

Edit `plugins/AdvancedMinecraftPlugin/config.yml`:
```yaml
database:
  mongo_uri: "mongodb://localhost:27017"
  database_name: "minecraft_plugin"

redis:
  host: "localhost"
  port: 6379
  password: ""
  database: 0

plugin:
  economy:
    starting_balance: 1000.0
    currency_symbol: "$"
    max_balance: 1000000.0
    min_balance: 0.0
  inventory:
    auto_save_on_quit: true
    max_inventories_per_player: 10
  ender_chest:
    auto_save_on_quit: true
    max_ender_chests_per_player: 10
```

## Commands

### Admin Commands
- `/money get [player]` - Check balance
- `/money set <player> <amount>` - Set balance
- `/money add <player> <amount>` - Add money
- `/money remove <player> <amount>` - Remove money
- `/money transfer <from> <to> <amount>` - Transfer money

- `/inv save <player> [name]` - Save inventory
- `/inv load <player> [name]` - Load inventory
- `/inv delete <player> <name>` - Delete inventory
- `/inv list <player>` - List inventories

- `/ec save <player> [name]` - Save ender chest
- `/ec load <player> [name]` - Load ender chest
- `/ec delete <player> <name>` - Delete ender chest
- `/ec list <player>` - List ender chests

### Player Commands
- `/plugin balance` - Check own balance
- `/plugin inventory save <name>` - Save own inventory
- `/plugin inventory load <name>` - Load own inventory
- `/plugin inventory list` - List own inventories
- `/plugin enderchest save <name>` - Save own ender chest
- `/plugin enderchest load <name>` - Load own ender chest
- `/plugin enderchest list` - List own ender chests

## Permissions

- `advancedplugin.economy` - Economy commands
- `advancedplugin.inventory` - Inventory commands
- `advancedplugin.enderchest` - Ender chest commands
- `advancedplugin.player` - Player self-management
- `advancedplugin.*` - All permissions

## Building

```bash
./gradlew shadowJar
```

JAR file will be in `build/libs/AdvancedMinecraftPlugin-1.0.0.jar`

## gRPC API

Enable in config:
```yaml
grpc:
  enabled: true
  port: 9090
  secret_key: "your_secret_key"
```

API endpoints available for all economy, inventory, and ender chest operations.

## Made by Alexetrey ( support me :D)