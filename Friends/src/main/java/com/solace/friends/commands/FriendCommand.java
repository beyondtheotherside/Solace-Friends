package com.solace.friends.commands;

import com.solace.friends.FriendsPlugin;
import com.solace.friends.managers.FriendManager;
import com.solace.friends.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final FriendManager friendManager;
    private final FriendsPlugin plugin;

    public FriendCommand(FriendManager friendManager, FriendsPlugin plugin) {
        this.friendManager = friendManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String message = plugin.getConfig().getString("messages.players-only", "&cThis command can only be used by players!");
            sender.sendMessage(MessageUtils.colorize(message));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (args.length < 2) {
                    String usageMsg = plugin.getConfig().getString("messages.add-usage", "&cUsage: /friend add <player>");
                    player.sendMessage(MessageUtils.colorize(usageMsg));
                    return true;
                }
                handleAdd(player, args[1]);
                break;
            case "accept":
                if (args.length < 2) {
                    String usageMsg = plugin.getConfig().getString("messages.accept-usage", "&cUsage: /friend accept <player>");
                    player.sendMessage(MessageUtils.colorize(usageMsg));
                    return true;
                }
                handleAccept(player, args[1]);
                break;
            case "deny":
            case "decline":
                if (args.length < 2) {
                    String usageMsg = plugin.getConfig().getString("messages.deny-usage", "&cUsage: /friend deny <player>");
                    player.sendMessage(MessageUtils.colorize(usageMsg));
                    return true;
                }
                handleDeny(player, args[1]);
                break;
            case "requests":
            case "pending":
                handleRequests(player);
                break;

            case "remove":
            case "delete":
            case "rm":
                if (args.length < 2) {
                    String usageMsg = plugin.getConfig().getString("messages.remove-usage", "&cUsage: /friend remove <player>");
                    player.sendMessage(MessageUtils.colorize(usageMsg));
                    return true;
                }
                handleRemove(player, args[1]);
                break;

            case "list":
                handleList(player);
                break;

            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void handleAdd(Player player, String targetName) {
        if (targetName.equalsIgnoreCase(player.getName())) {
            String message = plugin.getConfig().getString("messages.add-self", "&cYou cannot add yourself as a friend!");
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            String message = plugin.getConfig().getString("messages.add-player-not-found", "&cPlayer '{player}' not found!")
                    .replace("{player}", targetName);
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        if (friendManager.areFriends(playerUUID, targetUUID)) {
            String message = plugin.getConfig().getString("messages.add-already-friends", "&e{player} is already your friend!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        if (friendManager.hasPendingRequest(targetUUID, playerUUID)) {
            String message = plugin.getConfig().getString("messages.request-already-sent", "&eYou already sent a request to {player}!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        if (friendManager.hasPendingRequest(playerUUID, targetUUID)) {
            String message = plugin.getConfig().getString("messages.request-already-received", "&e{player} has already sent you a request. Use /friend accept {player}!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        friendManager.addPendingRequest(targetUUID, playerUUID);
        String message = plugin.getConfig().getString("messages.request-sent", "&aFriend request sent to {player}!")
                .replace("{player}", target.getName());
        player.sendMessage(MessageUtils.colorize(message));

        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                String requestMessage = plugin.getConfig().getString("messages.request-received", "&e{player} sent you a friend request. Use /friend accept {player}!")
                        .replace("{player}", player.getName());
                targetPlayer.sendMessage(MessageUtils.colorize(requestMessage));
            }
        }
    }

    private void handleAccept(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            String message = plugin.getConfig().getString("messages.accept-player-not-found", "&cPlayer '{player}' not found!")
                    .replace("{player}", targetName);
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        if (!friendManager.hasPendingRequest(playerUUID, targetUUID)) {
            String message = plugin.getConfig().getString("messages.accept-no-request", "&cYou have no pending request from {player}!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        friendManager.removePendingRequest(playerUUID, targetUUID);
        friendManager.addFriendship(playerUUID, targetUUID);

        String message = plugin.getConfig().getString("messages.accept-success", "&aYou are now friends with {player}!")
                .replace("{player}", target.getName());
        player.sendMessage(MessageUtils.colorize(message));

        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                String notify = plugin.getConfig().getString("messages.accept-notify", "&a{player} accepted your friend request!")
                        .replace("{player}", player.getName());
                targetPlayer.sendMessage(MessageUtils.colorize(notify));
            }
        }
    }

    private void handleDeny(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            String message = plugin.getConfig().getString("messages.deny-player-not-found", "&cPlayer '{player}' not found!")
                    .replace("{player}", targetName);
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        if (!friendManager.hasPendingRequest(playerUUID, targetUUID)) {
            String message = plugin.getConfig().getString("messages.deny-no-request", "&cYou have no pending request from {player}!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        friendManager.removePendingRequest(playerUUID, targetUUID);
        String message = plugin.getConfig().getString("messages.deny-success", "&eYou denied {player}'s friend request.")
                .replace("{player}", target.getName());
        player.sendMessage(MessageUtils.colorize(message));

        if (target.isOnline()) {
            Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                String notify = plugin.getConfig().getString("messages.deny-notify", "&e{player} denied your friend request.")
                        .replace("{player}", player.getName());
                targetPlayer.sendMessage(MessageUtils.colorize(notify));
            }
        }
    }

    private void handleRequests(Player player) {
        Set<UUID> requesters = friendManager.getPendingRequests(player.getUniqueId());

        if (requesters.isEmpty()) {
            String message = plugin.getConfig().getString("messages.requests-none", "&eYou have no pending friend requests!");
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        String header = plugin.getConfig().getString("messages.requests-header", "&6=== Pending Friend Requests ===");
        player.sendMessage(MessageUtils.colorize(header));

        List<String> names = new ArrayList<>();
        for (UUID requesterUUID : requesters) {
            String name = friendManager.getPlayerName(requesterUUID);
            if (name != null) {
                names.add(name);
            }
        }
        if (!names.isEmpty()) {
            String listMsg = plugin.getConfig().getString("messages.requests-list", "&e{players}")
                    .replace("{players}", String.join(", ", names));
            player.sendMessage(MessageUtils.colorize(listMsg));
        }
    }

    private void handleRemove(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            String message = plugin.getConfig().getString("messages.remove-player-not-found", "&cPlayer '{player}' not found!")
                    .replace("{player}", targetName);
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        UUID targetUUID = target.getUniqueId();
        UUID playerUUID = player.getUniqueId();

        if (!friendManager.areFriends(playerUUID, targetUUID)) {
            String message = plugin.getConfig().getString("messages.remove-not-friends", "&c{player} is not your friend!")
                    .replace("{player}", target.getName());
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        friendManager.removeFriendship(playerUUID, targetUUID);
        String message = plugin.getConfig().getString("messages.remove-success", "&aRemoved {player} from your friends list!")
                .replace("{player}", target.getName());
        player.sendMessage(MessageUtils.colorize(message));
    }

    private void handleList(Player player) {
        Set<UUID> friends = friendManager.getFriends(player.getUniqueId());

        if (friends.isEmpty()) {
            String message = plugin.getConfig().getString("messages.list-no-friends", "&eYou have no friends!");
            player.sendMessage(MessageUtils.colorize(message));
            return;
        }

        List<String> onlineFriends = new ArrayList<>();
        List<String> offlineFriends = new ArrayList<>();

        for (UUID friendUUID : friends) {
            Player friend = Bukkit.getPlayer(friendUUID);
            String friendName = friendManager.getPlayerName(friendUUID);
            
            if (friend != null && friend.isOnline()) {
                onlineFriends.add(friendName);
            } else {
                offlineFriends.add(friendName);
            }
        }

        String header = plugin.getConfig().getString("messages.list-header", "&6=== Your Friends ===");
        player.sendMessage(MessageUtils.colorize(header));
        
        if (!onlineFriends.isEmpty()) {
            String onlineMsg = plugin.getConfig().getString("messages.list-online-prefix", "&aOnline ({count}): &f")
                    .replace("{count}", String.valueOf(onlineFriends.size()));
            player.sendMessage(MessageUtils.colorize(onlineMsg + String.join(", ", onlineFriends)));
        }
        
        if (!offlineFriends.isEmpty()) {
            String offlineMsg = plugin.getConfig().getString("messages.list-offline-prefix", "&7Offline ({count}): &f")
                    .replace("{count}", String.valueOf(offlineFriends.size()));
            player.sendMessage(MessageUtils.colorize(offlineMsg + String.join(", ", offlineFriends)));
        }
    }

    private void sendUsage(Player player) {
        String header = plugin.getConfig().getString("messages.usage-header", "&6=== Friend Commands ===");
        String usageAdd = plugin.getConfig().getString("messages.usage-add", "&e/friend add <player> &7- Add a friend");
        String usageAccept = plugin.getConfig().getString("messages.usage-accept", "&e/friend accept <player> &7- Accept a friend request");
        String usageDeny = plugin.getConfig().getString("messages.usage-deny", "&e/friend deny <player> &7- Deny a friend request");
        String usageRequests = plugin.getConfig().getString("messages.usage-requests", "&e/friend requests &7- View pending requests");
        String usageRemove = plugin.getConfig().getString("messages.usage-remove", "&e/friend remove <player> &7- Remove a friend");
        String usageList = plugin.getConfig().getString("messages.usage-list", "&e/friend list &7- List all your friends");
        
        player.sendMessage(MessageUtils.colorize(header));
        player.sendMessage(MessageUtils.colorize(usageAdd));
        player.sendMessage(MessageUtils.colorize(usageAccept));
        player.sendMessage(MessageUtils.colorize(usageDeny));
        player.sendMessage(MessageUtils.colorize(usageRequests));
        player.sendMessage(MessageUtils.colorize(usageRemove));
        player.sendMessage(MessageUtils.colorize(usageList));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("add", "accept", "deny", "requests", "remove", "list"));
            String input = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(input));
            return completions;
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("add") || subCommand.equals("remove") || subCommand.equals("delete") || subCommand.equals("rm")
                    || subCommand.equals("accept") || subCommand.equals("deny") || subCommand.equals("decline")) {
                List<String> playerNames = new ArrayList<>();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.getName().equalsIgnoreCase(((Player) sender).getName())) {
                        playerNames.add(onlinePlayer.getName());
                    }
                }
                String input = args[1].toLowerCase();
                playerNames.removeIf(s -> !s.toLowerCase().startsWith(input));
                return playerNames;
            }
        }

        return Collections.emptyList();
    }
}
