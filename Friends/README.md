# Friends Plugin

A Minecraft Paper plugin for version 1.21.1 that allows players to add friends and receive notifications when their friends come online.

## Features

- **Friend Management**: Add and remove friends using simple commands
- **Online Notifications**: Get notified in the action bar when a friend comes online
- **Friend List**: View all your friends and see who's online
- **Persistent Storage**: Friend data is saved to a YAML file
- **Configurable**: Customize messages and notification settings

## Commands

- `/friend add <player>` - Add a player as a friend
- `/friend remove <player>` - Remove a player from your friends list
- `/friend list` - List all your friends (shows online/offline status)

**Aliases**: `/f`, `/friends`

## Building

1. Make sure you have Maven installed
2. Run `mvn clean package` in the project directory
3. The compiled JAR will be in the `target/` directory

## Installation

1. Place the compiled JAR file in your server's `plugins/` folder
2. Restart your server
3. The plugin will create a `config.yml` file in `plugins/Friends/`
4. Customize the config as needed

## Configuration

The `config.yml` file contains the following options:

- `notifications.chat-message`: Whether to send a chat message in addition to action bar (default: false)
- `notifications.action-bar-duration`: Duration to show action bar message in ticks (default: 60)
- `messages.friend-online`: Message shown when a friend comes online (supports `{player}` placeholder)
- `messages.friends-online`: Message shown when you join and have friends online (supports `{count}` placeholder)
- `storage.data-file`: Name of the file storing friend data (default: friends.yml)
- `storage.auto-save-interval`: Auto-save interval in seconds (0 to disable, default: 300)

## Data Storage

Friend data is stored in `plugins/Friends/friends.yml`. The file is automatically saved when:
- A friend is added or removed
- The server shuts down
- At the configured auto-save interval

## Requirements

- Paper/Spigot 1.21.1 or higher
- Java 21 or higher
