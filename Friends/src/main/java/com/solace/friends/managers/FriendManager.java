package com.solace.friends.managers;

import com.solace.friends.FriendsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FriendManager {

    private final FriendsPlugin plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final Map<UUID, Set<UUID>> friendsMap; // Player UUID -> Set of friend UUIDs
    private final Map<UUID, Set<UUID>> pendingRequests; // Target UUID -> Set of requester UUIDs

    public FriendManager(FriendsPlugin plugin) {
        this.plugin = plugin;
        this.friendsMap = new HashMap<>();
        this.pendingRequests = new HashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "friends.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create friends.yml: " + e.getMessage());
            }
        }

        loadData();
    }

    public void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        friendsMap.clear();
        pendingRequests.clear();

        if (dataConfig.getConfigurationSection("friends") != null) {
            for (String playerUuid : dataConfig.getConfigurationSection("friends").getKeys(false)) {
                UUID playerUUID = UUID.fromString(playerUuid);
                Set<UUID> friends = new HashSet<>();
                
                List<String> friendUuids = dataConfig.getStringList("friends." + playerUuid);
                for (String friendUuid : friendUuids) {
                    try {
                        friends.add(UUID.fromString(friendUuid));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in friends.yml: " + friendUuid);
                    }
                }
                
                friendsMap.put(playerUUID, friends);
            }
        }

        if (dataConfig.getConfigurationSection("pending") != null) {
            for (String playerUuid : dataConfig.getConfigurationSection("pending").getKeys(false)) {
                UUID playerUUID = UUID.fromString(playerUuid);
                Set<UUID> requesters = new HashSet<>();

                List<String> requesterUuids = dataConfig.getStringList("pending." + playerUuid);
                for (String requesterUuid : requesterUuids) {
                    try {
                        requesters.add(UUID.fromString(requesterUuid));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in friends.yml pending: " + requesterUuid);
                    }
                }

                pendingRequests.put(playerUUID, requesters);
            }
        }
    }

    public void saveData() {
        dataConfig.set("friends", null); // Clear existing data
        dataConfig.set("pending", null);

        for (Map.Entry<UUID, Set<UUID>> entry : friendsMap.entrySet()) {
            List<String> friendUuids = new ArrayList<>();
            for (UUID friendUuid : entry.getValue()) {
                friendUuids.add(friendUuid.toString());
            }
            dataConfig.set("friends." + entry.getKey().toString(), friendUuids);
        }

        for (Map.Entry<UUID, Set<UUID>> entry : pendingRequests.entrySet()) {
            List<String> requesterUuids = new ArrayList<>();
            for (UUID requesterUuid : entry.getValue()) {
                requesterUuids.add(requesterUuid.toString());
            }
            dataConfig.set("pending." + entry.getKey().toString(), requesterUuids);
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save friends.yml: " + e.getMessage());
        }
    }

    public boolean addFriend(UUID playerUUID, UUID friendUUID) {
        if (playerUUID.equals(friendUUID)) {
            return false; // Can't add yourself
        }

        friendsMap.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(friendUUID);
        saveData();
        return true;
    }

    public boolean addFriendship(UUID playerUUID, UUID friendUUID) {
        if (playerUUID.equals(friendUUID)) {
            return false;
        }

        boolean changed = false;
        changed |= friendsMap.computeIfAbsent(playerUUID, k -> new HashSet<>()).add(friendUUID);
        changed |= friendsMap.computeIfAbsent(friendUUID, k -> new HashSet<>()).add(playerUUID);
        if (changed) {
            saveData();
        }
        return changed;
    }

    public boolean removeFriend(UUID playerUUID, UUID friendUUID) {
        boolean removed = removeFriendNoSave(playerUUID, friendUUID);
        if (removed) {
            saveData();
        }
        return removed;
    }

    public boolean removeFriendship(UUID playerUUID, UUID friendUUID) {
        boolean changed = false;
        changed |= removeFriendNoSave(playerUUID, friendUUID);
        changed |= removeFriendNoSave(friendUUID, playerUUID);
        if (changed) {
            saveData();
        }
        return changed;
    }

    private boolean removeFriendNoSave(UUID playerUUID, UUID friendUUID) {
        Set<UUID> friends = friendsMap.get(playerUUID);
        if (friends != null && friends.remove(friendUUID)) {
            if (friends.isEmpty()) {
                friendsMap.remove(playerUUID);
            }
            return true;
        }
        return false;
    }

    public boolean addPendingRequest(UUID targetUUID, UUID requesterUUID) {
        if (targetUUID.equals(requesterUUID)) {
            return false;
        }
        boolean added = pendingRequests.computeIfAbsent(targetUUID, k -> new HashSet<>()).add(requesterUUID);
        if (added) {
            saveData();
        }
        return added;
    }

    public boolean hasPendingRequest(UUID targetUUID, UUID requesterUUID) {
        Set<UUID> requesters = pendingRequests.get(targetUUID);
        return requesters != null && requesters.contains(requesterUUID);
    }

    public boolean removePendingRequest(UUID targetUUID, UUID requesterUUID) {
        Set<UUID> requesters = pendingRequests.get(targetUUID);
        if (requesters != null && requesters.remove(requesterUUID)) {
            if (requesters.isEmpty()) {
                pendingRequests.remove(targetUUID);
            }
            saveData();
            return true;
        }
        return false;
    }

    public Set<UUID> getPendingRequests(UUID targetUUID) {
        return pendingRequests.getOrDefault(targetUUID, new HashSet<>());
    }

    public Set<UUID> getFriends(UUID playerUUID) {
        return friendsMap.getOrDefault(playerUUID, new HashSet<>());
    }

    public boolean areFriends(UUID playerUUID, UUID friendUUID) {
        Set<UUID> friends = friendsMap.get(playerUUID);
        return friends != null && friends.contains(friendUUID);
    }

    public Set<UUID> getFriendsWhoHavePlayerAsFriend(UUID playerUUID) {
        Set<UUID> result = new HashSet<>();
        for (Map.Entry<UUID, Set<UUID>> entry : friendsMap.entrySet()) {
            if (entry.getValue().contains(playerUUID)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public String getPlayerName(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            return player.getName();
        }
        // Try to get from offline player
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}
