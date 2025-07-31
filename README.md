# Eclipse Framework

<div align="center">

![Eclipse Logo](https://img.shields.io/badge/Eclipse-Framework-orange?style=for-the-badge&logo=minecraft)

**A professional, minimal plugin framework for Minecraft servers**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-17+-green.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Spigot](https://img.shields.io/badge/Spigot-1.21.8-yellow.svg)](https://www.spigotmc.org/)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

[Features](#features) • [Installation](#installation) • [Documentation](#documentation) • [API](#api) • [Contributing](#contributing)

</div>

---

## 📋 Overview

Eclipse Framework is a comprehensive, production-ready plugin framework designed for Minecraft servers. It provides essential systems like configuration management, database connectivity, caching, permissions, security, and migrations - all in a single, lightweight package.

Built with **professional development practices** in mind, Eclipse offers both standalone functionality and a powerful API for other plugins to leverage.

## ✨ Features

### 🏗️ Core Systems
- **Configuration Management** - YAML configuration with hot-reloading
- **Task Queue System** - Async and sync task execution with priorities
- **Event System** - Custom event handling with listener management
- **Message Manager** - Localization support with placeholders and color codes

### 💾 Data Management
- **Multi-Database Support** - MySQL, SQLite, and JSON storage
- **Player Cache System** - Efficient player data caching with auto-cleanup
- **Migration System** - Database and configuration versioning
- **Connection Pooling** - HikariCP for optimal database performance

### 🔐 Security & Permissions
- **Permission System** - Groups, inheritance, and temporary permissions
- **Security Manager** - Rate limiting, IP blocking, and anti-spam
- **Command Cooldowns** - Configurable per-command cooldowns
- **Violation Tracking** - Automatic security violation monitoring

### 🛠️ Developer Tools
- **Unified API** - Single access point for all framework features
- **Health Monitoring** - System health checks and diagnostics
- **Automatic Backups** - Scheduled backups with cleanup
- **Professional Logging** - Structured logging with performance metrics

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Spigot/Paper 1.21+
- Maven (for building)

### Installation

1. **Download the latest release**
   ```bash
   wget https://github.com/T3CC4/Eclipse/releases/latest/download/Eclipse-1.0.0.jar
   ```

2. **Place in your plugins folder**
   ```bash
   cp Eclipse-1.0.0.jar /path/to/server/plugins/
   ```

3. **Start your server**
   ```bash
   java -jar spigot-1.21.jar
   ```

4. **Configure the framework** (optional)
   - Edit `plugins/Eclipse/config.yml`
   - Configure database in `plugins/Eclipse/database.yml`
   - Customize messages in `plugins/Eclipse/messages.yml`

## 📖 Configuration

### Database Configuration
```yaml
# Database type: mysql, sqlite, json
type: "mysql"

mysql:
  host: "localhost"
  port: 3306
  database: "eclipse"
  username: "root"
  password: "your_password"
```

### Security Configuration
```yaml
rate-limiting:
  max-logins-per-minute: 10
  max-commands-per-second: 5
  max-chat-per-second: 3

cooldowns:
  tp: 2000
  home: 5000
  heal: 60000
```

### Permission Groups
```yaml
groups:
  admin:
    display-name: "Administrator"
    priority: 100
    prefix: "&c[Admin] "
    permissions:
      - "*"
```

## 🔧 API Usage

### Getting Started
```java
// Get the Eclipse API
EclipseAPI api = Eclipse.getAPI();

// Use database functionality
api.setJSONData("player_" + uuid, playerData);
PlayerData data = api.getJSONData("player_" + uuid, PlayerData.class);

// Send messages
api.sendMessage(player, "welcome.message", Map.of("player", player.getName()));

// Manage permissions
api.addPlayerPermission(uuid, "eclipse.admin");
api.addPlayerToGroup(uuid, "vip");

// Security features
api.setCooldown(uuid, "heal", 30000);
api.blockIP(ipAddress, "Spamming", adminUUID, null);
```

### Event System
```java
// Register a custom event listener
api.registerListener(new EventListener<PlayerJoinEvent>() {
    @Override
    public void onEvent(PlayerJoinEvent event) {
        // Handle custom logic
    }
    
    @Override
    public Class<PlayerJoinEvent> getEventType() {
        return PlayerJoinEvent.class;
    }
});
```

### Database Operations
```java
// Async database operations
api.executeUpdateAsync("INSERT INTO users (uuid, name) VALUES (?, ?)", 
    uuid.toString(), playerName)
    .thenRun(() -> System.out.println("Player saved!"));

// JSON storage
api.setJSONDataAsync("config_" + key, configData)
    .thenAccept(result -> System.out.println("Config saved!"));
```

## 📊 Framework Statistics

```java
// Get comprehensive framework info
String info = api.getFrameworkInfo();
System.out.println(info);

// Perform health check
EclipseAPI.HealthCheckResult health = api.performHealthCheck();
if (health.isHealthy()) {
    System.out.println("All systems operational!");
}

// Get security stats
SecurityManager.SecurityStats stats = api.getSecurityStats();
System.out.println("Blocked IPs: " + stats.getBlockedIPs());
```

## 🏗️ Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/T3CC4/Eclipse.git
   cd Eclipse
   ```

2. **Build with Maven**
   ```bash
   mvn clean package
   ```

3. **Find the JAR**
   ```bash
   ls target/Eclipse-*.jar
   ```

## 📁 Project Structure

```
Eclipse/
├── src/main/java/de/tecca/eclipse/
│   ├── Eclipse.java                 # Main plugin class
│   ├── api/EclipseAPI.java         # Unified API
│   ├── cache/PlayerCache.java      # Player data caching
│   ├── config/ConfigManager.java   # Configuration management
│   ├── database/DatabaseManager.java # Multi-database support
│   ├── event/EventManager.java     # Custom event system
│   ├── message/MessageManager.java # Localization & messaging
│   ├── migration/MigrationSystem.java # Database migrations
│   ├── permission/PermissionManager.java # Permission system
│   ├── queue/TaskQueue.java        # Task execution
│   └── security/SecurityManager.java # Security features
├── src/main/resources/
│   ├── config.yml                  # Main configuration
│   ├── database.yml               # Database settings
│   ├── messages.yml               # Localization
│   ├── permissions.yml            # Permission groups
│   ├── security.yml               # Security settings
│   └── plugin.yml                 # Plugin metadata
└── pom.xml                        # Maven configuration
```

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and test thoroughly
4. Commit your changes: `git commit -m 'Add amazing feature'`
5. Push to the branch: `git push origin feature/amazing-feature`
6. Open a Pull Request

### Code Style
- Follow Oracle Java conventions
- Use meaningful variable and method names
- Include JavaDoc for public APIs
- Maintain backwards compatibility

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: [GitHub Issues](https://github.com/T3CC4/Eclipse/issues)
- **Discussions**: [GitHub Discussions](https://github.com/T3CC4/Eclipse/discussions)
- **Wiki**: [Documentation Wiki](https://github.com/T3CC4/Eclipse/wiki)

## 📈 Roadmap

- [ ] Web API Gateway
- [ ] Redis Support
- [ ] Plugin Metrics Dashboard
- [ ] Advanced Permission Conditions
- [ ] Cluster Support
- [ ] GUI Configuration Editor

## 🙏 Acknowledgments

- **Spigot Team** - For the excellent server software
- **HikariCP** - For reliable connection pooling
- **Gson** - For JSON processing
- **Maven** - For build management

---

<div align="center">

**Made with ❤️ for the Minecraft community**

[⭐ Star this repo](https://github.com/T3CC4/Eclipse) if you find it useful!

</div>
