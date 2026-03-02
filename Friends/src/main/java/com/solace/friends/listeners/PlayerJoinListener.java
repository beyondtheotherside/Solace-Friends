package com.solace.friends.listeners;

import com.solace.friends.FriendsPlugin;
import com.solace.friends.managers.FriendManager;
import com.solace.friends.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;

import java.util.Set;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final FriendManager friendManager;
    private final FriendsPlugin plugin;

    public PlayerJoinListener(FriendManager friendManager, FriendsPlugin plugin) {
        this.friendManager = friendManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        UUID joinedPlayerUUID = joinedPlayer.getUniqueId();

        // Get all players who have this player as a friend
        Set<UUID> friendsWhoHavePlayer = friendManager.getFriendsWhoHavePlayerAsFriend(joinedPlayerUUID);

        // Send notification to all online friends
        for (UUID friendUUID : friendsWhoHavePlayer) {
            Player friend = Bukkit.getPlayer(friendUUID);
            if (friend != null && friend.isOnline()) {
                // Send action bar notification
                String message = plugin.getConfig().getString("messages.friend-online", "&a{player} is now online!")
                        .replace("{player}", joinedPlayer.getName());
                
                sendActionBarForDuration(friend, MessageUtils.colorize(message));
                
                // Optionally send a chat message too (configurable)
                if (plugin.getConfig().getBoolean("notifications.chat-message", false)) {
                    friend.sendMessage(MessageUtils.colorize(message));
                }
            }
        }

        // Also notify the joined player about their online friends
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (joinedPlayer.isOnline()) {
                Set<UUID> playerFriends = friendManager.getFriends(joinedPlayerUUID);
                int onlineCount = 0;
                
                for (UUID friendUUID : playerFriends) {
                    Player friend = Bukkit.getPlayer(friendUUID);
                    if (friend != null && friend.isOnline()) {
                        onlineCount++;
                    }
                }
        // Message about count of friends online        
                if (onlineCount > 0) {
                    String message = plugin.getConfig().getString("messages.friends-online", "&aYou have {count} friend(s) online!")
                            .replace("{count}", String.valueOf(onlineCount));
                    sendActionBarForDuration(joinedPlayer, MessageUtils.colorize(message));
                }

                Set<UUID> pendingRequests = friendManager.getPendingRequests(joinedPlayerUUID);
                if (!pendingRequests.isEmpty()) {
                    String message = plugin.getConfig().getString("messages.pending-requests", "&eYou have {count} pending friend request(s). Use /friend requests.")
                            .replace("{count}", String.valueOf(pendingRequests.size()));
                    joinedPlayer.sendMessage(MessageUtils.colorize(message));
                }
            }
        }, 20L); // 1 second delay
    }

    private void sendActionBarForDuration(Player player, Component message) {
        int durationTicks = plugin.getConfig().getInt("notifications.action-bar-duration", 60);
        if (durationTicks <= 0) {
            return;
        }

        player.sendActionBar(message);
        if (durationTicks <= 20) {
            return;
        }

        new BukkitRunnable() {
            int remainingTicks = durationTicks - 20;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                player.sendActionBar(message);
                remainingTicks -= 20;
                if (remainingTicks <= 0) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}
