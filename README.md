# CoreTuff

CoreTuff is a modern, performance-focused Minecraft core plugin for **Paper and Folia**, designed to provide essential server management, moderation, teleportation, economy, utility, and quality-of-life features in one powerful and scalable package.

Built with performance and reliability in mind, CoreTuff is designed to scale from small community servers to larger production environments while keeping its architecture clean, modular, and maintainable.

# Key Features

## Teleportation

CoreTuff provides a complete teleportation system with configurable behavior.

| Command | Description |
| --- | --- |
| `/tpa` | Request to teleport to another player. |
| `/tphere` | Request another player to teleport to you. |
| `/tpaccept` | Accept a teleport request. |
| `/tpdeny` | Deny a teleport request. |
| `/rtp` | Teleport to a random location. |
| `/back` | Return to your previous location. |
| `/tpignore` | Ignore teleport requests from a player. |
| `/tpunignore` | Stop ignoring a player's teleport requests. |

Teleportation supports configurable cooldowns, delays, movement cancellation, countdowns, sounds, and Folia-compatible handling.

---

## Moderation

A complete moderation system for managing your server.

| Command | Description |
| --- | --- |
| `/ban` | Permanently ban a player. |
| `/tempban` | Temporarily ban a player. |
| `/ban-ip` | Ban a player's IP address. |
| `/kick` | Remove a player from the server. |
| `/warn` | Issue a configurable warning. |
| `/pid` | Manage punishment IDs and moderation history. |
| `/pid view <id>` | View punishment information. |
| `/pid pardon <id> <reason>` | Pardon a punishment. |

Includes configurable punishment behavior, warning systems, punishment tracking, and vanilla ban synchronization.

---

## Economy

CoreTuff includes a persistent server economy system.

| Command | Description |
| --- | --- |
| `/balance` | View your current balance. |
| `/pay` | Transfer money to another player. |
| `/eco` | Manage the server economy. |

Features included:

- Persistent H2-backed balances
- Transfer taxes
- Interest accumulation
- Transaction logging
- Balance limits
- Configurable currency formatting
- Economy leaderboards
- Vault integration

CoreTuff can integrate with compatible Vault-based economy plugins and systems.

---

## Homes & Warps

Manage player homes and server warps with persistent storage.

Features include:

- Persistent storage
- Configurable teleport delays
- Permission-based limits
- Economy integration
- Configurable warp behavior

---

## Kits

Create configurable kits using YAML definitions.

Features include:

- Custom kit definitions
- Cooldowns
- Player targeting
- Configurable kit contents

---

## Utility Commands

CoreTuff includes a collection of everyday server utilities.

Available utilities include:

`/fly`  
`/heal`  
`/feed`  
`/hunger`  
`/hat`

Gamemode shortcuts:

`/gmc`  
`/gma`  
`/gms`  
`/gmsp`

CoreTuff also provides virtual workbenches and utility interfaces including:

- Crafting Table
- Anvil
- Stonecutter
- Smithing Table
- Loom
- Cartography Table
- Enchanting Table
- Grindstone

---

## Server Security

CoreTuff provides optional server-side visibility and command protection features.

Includes:

- Plugin hiding
- Command hiding
- Permission-aware command visibility
- Protocol-based command protection
- Optional plugin visibility controls

---

## CoreTuff Dashboard

CoreTuff includes a Web Dashboard for managing and monitoring your server.

Access the Dashboard using:

`/ct dash`

The Dashboard provides:

- Live server statistics
- TPS and MSPT monitoring
- CPU and memory information
- Plugin information
- Integration status
- CoreTuff subsystem health
- Configuration management
- Incident monitoring
- Server events
- Diagnostics

CoreTuff can automatically detect performance problems and report them through the Dashboard.

---

## Incident & Performance Monitoring

CoreTuff doesn't just display server statistics — it can monitor them.

The monitoring system can detect:

- TPS degradation
- MSPT spikes
- CPU pressure
- Memory pressure
- Database problems
- Module failures
- Integration issues
- Exceptions
- Dashboard events

Detected problems can be tracked as incidents and automatically marked as resolved when the server recovers.

---

## Diagnostics

CoreTuff includes built-in diagnostic tools for troubleshooting.

`/ct dump`

Generates a comprehensive diagnostic report containing server, runtime, database, configuration, integration, permission, and exception information.

`/ct dumpdata`

Exports CoreTuff database information for debugging and support.

`/ct bug`

Provides quick access to the CoreTuff issue tracker.

---

## PlaceholderAPI

CoreTuff provides PlaceholderAPI integration with economy and CoreTuff-related placeholders.

---

## Integrations

CoreTuff can detect and integrate with supported server APIs when they are available.

Optional integrations are automatically detected rather than requiring unnecessary dependencies when they are not being used.

---

## Performance & Compatibility

CoreTuff is designed with performance and modern Minecraft server software in mind.

Supported platforms include:

- Paper
- Folia
- Paper-based server software

CoreTuff uses asynchronous processing where appropriate and provides Folia-compatible scheduling and handling.

---

# Why CoreTuff?

CoreTuff isn't designed to be just another teleport plugin.

It is built around a simple idea:

**One core. Everything your server actually needs.**

Instead of installing a collection of separate plugins for basic server functionality, CoreTuff brings essential systems together under one maintainable platform.

Performance-first architecture.

Modern Paper & Folia compatibility.

Persistent storage.

Configurable systems.

Server monitoring.

Built-in diagnostics.

And a Dashboard that actually tells you when something is going wrong.

Welcome to CoreTuff.
