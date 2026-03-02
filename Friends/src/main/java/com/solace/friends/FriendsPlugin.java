package com.solace.friends;

import com.solace.friends.commands.FriendCommand;
import com.solace.friends.listeners.PlayerJoinListener;
import com.solace.friends.managers.FriendManager;
import org.bukkit.plugin.java.JavaPlugin;

public class FriendsPlugin extends JavaPlugin {

    private FriendManager friendManager;
    private int autoSaveTaskId = -1;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize friend manager
        friendManager = new FriendManager(this);

        // Register command
        getCommand("friend").setExecutor(new FriendCommand(friendManager, this));
        getCommand("friend").setTabCompleter(new FriendCommand(friendManager, this));

        // Register event listener
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(friendManager, this), this);

        // Setup auto-save if enabled
        int autoSaveInterval = getConfig().getInt("storage.auto-save-interval", 300);
        if (autoSaveInterval > 0) {
            long ticks = autoSaveInterval * 20L; // Convert seconds to ticks
            autoSaveTaskId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                friendManager.saveData();
            }, ticks, ticks).getTaskId();
        }

        getLogger().info("Solace Friends has been successfully enabled!");
    }
// auto-save if disabled
    @Override
    public void onDisable() {
        // Cancel auto-save task
        if (autoSaveTaskId != -1) {
            getServer().getScheduler().cancelTask(autoSaveTaskId);
        }

        // Save friend data
        if (friendManager != null) {
            friendManager.saveData();
        }

        getLogger().info("Solace Friends has been successfully disabled!");
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }
}

// A plugin by otherside - The Solace Project 2026