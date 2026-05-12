package com.ngtmehros.nigtmehrosvanish;

import com.ngtmehros.nigtmehrosvanish.BanRecord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MultiAccountDetection implements Listener {

    private final NigtmehrosBanPlugin plugin;
    private final BanManager banManager;
    private final Map<String, Set<UUID>> ipToUuids = new ConcurrentHashMap<>();
    private final Map<UUID, String> uuidToIp = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> linkedAccounts = new ConcurrentHashMap<>();

    public MultiAccountDetection(NigtmehrosBanPlugin plugin, BanManager banManager) {
        this.plugin = plugin;
        this.banManager = banManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID uuid = event.getUniqueId();
        String ip = event.getAddress().getHostAddress();

        // Prüfe auf Multi-Accounts
        checkForMultiAccounts(uuid, ip, event.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String ip = player.getAddress().getAddress().getHostAddress();

        // Account-Daten aktualisieren
        updateAccountData(uuid, ip);
    }

    private void checkForMultiAccounts(UUID uuid, String ip, String playerName) {
        // Prüfe ob IP bereits bekannt ist
        Set<UUID> knownAccounts = ipToUuids.getOrDefault(ip, new HashSet<>());
        
        // Entferne alte Einträge (accounts die nicht mehr existieren)
        knownAccounts.removeIf(accountUuid -> {
            BanRecord record = banManager.getActiveBan(accountUuid);
            return record == null; // Nur entfernen wenn nicht gebannt
        });

        // Prüfe auf gebannte Accounts auf gleicher IP
        for (UUID accountUuid : knownAccounts) {
            BanRecord record = banManager.getActiveBan(accountUuid);
            if (record != null) {
                // Multi-Account mit gebanntem Account entdeckt!
                handleMultiAccountDetection(uuid, playerName, ip, accountUuid, record);
                return;
            }
        }

        // Prüfe auf zu viele Accounts von gleicher IP
        if (knownAccounts.size() >= plugin.getConfig().getInt("multi-account.max-accounts-per-ip", 3)) {
            handleTooManyAccounts(uuid, playerName, ip, knownAccounts);
        }
    }

    private void handleMultiAccountDetection(UUID uuid, String playerName, String ip, UUID bannedUuid, BanRecord bannedRecord) {
        // Webhook senden
        plugin.getDiscordWebhookService().sendMultiAccountAlert(
            playerName, 
            uuid.toString(), 
            ip, 
            bannedRecord.playerName(), 
            bannedRecord.reason()
        );

        // Admins benachrichtigen
        notifyAdmins(
            "⚠️ Multi-Account Detection: " + playerName + " versucht mit IP " + ip + 
            " beizutreten. Gebannter Account: " + bannedRecord.playerName()
        );

        // Optional: Automatischer Bann
        if (plugin.getConfig().getBoolean("multi-account.auto-ban-on-detection", false)) {
            String reason = "Multi-Account Detection (Gebannter Account: " + bannedRecord.playerName() + ")";
            banManager.ipBan(playerName, "CrimsonBann", "perm", reason);
        }
    }

    private void handleTooManyAccounts(UUID uuid, String playerName, String ip, Set<UUID> knownAccounts) {
        // Webhook senden
        plugin.getDiscordWebhookService().sendTooManyAccountsAlert(
            playerName, 
            uuid.toString(), 
            ip, 
            knownAccounts.size()
        );

        // Admins benachrichtigen
        notifyAdmins(
            "⚠️ Zu viele Accounts: " + playerName + " (" + knownAccounts.size() + 
            " Accounts) von IP " + ip
        );
    }

    private void updateAccountData(UUID uuid, String ip) {
        // IP-Zuordnung aktualisieren
        ipToUuids.computeIfAbsent(ip, k -> new HashSet<>()).add(uuid);
        uuidToIp.put(uuid, ip);

        // Linked Accounts aktualisieren
        Set<UUID> accountsWithSameIp = ipToUuids.get(ip);
        for (UUID otherUuid : accountsWithSameIp) {
            if (!otherUuid.equals(uuid)) {
                linkedAccounts.computeIfAbsent(uuid, k -> new HashSet<>()).add(otherUuid);
                linkedAccounts.computeIfAbsent(otherUuid, k -> new HashSet<>()).add(uuid);
            }
        }
    }

    private void notifyAdmins(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("crimisonbann.admin")) {
                player.sendMessage("§c[CrimsonBann] §f" + message);
            }
        }
    }

    public Set<UUID> getLinkedAccounts(UUID uuid) {
        return linkedAccounts.getOrDefault(uuid, new HashSet<>());
    }

    public String getPlayerIP(UUID uuid) {
        return uuidToIp.get(uuid);
    }

    public void removeAccountData(UUID uuid) {
        String ip = uuidToIp.remove(uuid);
        if (ip != null) {
            Set<UUID> accounts = ipToUuids.get(ip);
            if (accounts != null) {
                accounts.remove(uuid);
                if (accounts.isEmpty()) {
                    ipToUuids.remove(ip);
                }
            }
        }
        
        linkedAccounts.remove(uuid);
        for (Set<UUID> linked : linkedAccounts.values()) {
            linked.remove(uuid);
        }
    }
}
