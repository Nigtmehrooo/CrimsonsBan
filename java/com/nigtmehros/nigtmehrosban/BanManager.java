package com.nigtmehros.nigtmehrosban;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class BanManager {

    public static final String BAN_PERMISSION = "nigtmehrosban.ban";
    public static final String UNBAN_PERMISSION = "nigtmehrosban.unban";
    public static final String BYPASS_PERMISSION = "nigtmehrosban.bypass";

    private final NigtmehrosBanPlugin plugin;
    private final File bansFile;
    private final YamlConfiguration bansConfig;

    public BanManager(NigtmehrosBanPlugin plugin) {
        this.plugin = plugin;
        this.bansFile = new File(plugin.getDataFolder(), "bans.yml");
        this.bansConfig = YamlConfiguration.loadConfiguration(bansFile);
    }

    public String ban(String targetName, String bannedBy, String durationInput, String reason) {
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(targetName);
        }
        if (target == null || target.getUniqueId() == null) {
            return "Dieser Spieler wurde nicht gefunden. Der Spieler muss online sein oder bereits auf dem Server gewesen sein.";
        }

        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().hasPermission(BYPASS_PERMISSION)) {
            return "Dieser Spieler kann nicht gebannt werden.";
        }

        ParsedDuration parsedDuration = parseDuration(durationInput);
        if (!parsedDuration.valid()) {
            return "Ungueltige Dauer. Nutze z.B. 30m, 12h, 7d oder perm.";
        }

        UUID uuid = target.getUniqueId();
        long now = System.currentTimeMillis();
        long expiresAt = parsedDuration.permanent() ? 0L : now + parsedDuration.durationMillis();

        String ipAddress = null;
        if (target.isOnline() && target.getPlayer() != null) {
            ipAddress = target.getPlayer().getAddress() != null ? 
                target.getPlayer().getAddress().getAddress().getHostAddress() : null;
        }

        bansConfig.set(uuid + ".name", targetName);
        bansConfig.set(uuid + ".bannedBy", bannedBy);
        bansConfig.set(uuid + ".reason", reason);
        bansConfig.set(uuid + ".createdAt", now);
        bansConfig.set(uuid + ".expiresAt", expiresAt);
        bansConfig.set(uuid + ".ipAddress", ipAddress);
        bansConfig.set(uuid + ".isIpBan", false);
        save();

        BanRecord record = new BanRecord(uuid, targetName, bannedBy, reason, now, expiresAt, ipAddress, false);
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            onlineTarget.kick(net.kyori.adventure.text.Component.text(createBanScreen(record)));
        }

        plugin.getDiscordWebhookService().sendBan(record);
        return null;
    }

    public String ipBan(String targetName, String bannedBy, String durationInput, String reason) {
        OfflinePlayer target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            target = Bukkit.getOfflinePlayerIfCached(targetName);
        }
        if (target == null || target.getUniqueId() == null) {
            return "Dieser Spieler wurde nicht gefunden.";
        }

        if (target.isOnline() && target.getPlayer() != null && target.getPlayer().hasPermission(BYPASS_PERMISSION)) {
            return "Dieser Spieler kann nicht gebannt werden.";
        }

        ParsedDuration parsedDuration = parseDuration(durationInput);
        if (!parsedDuration.valid()) {
            return "Ungueltige Dauer. Nutze z.B. 30m, 12h, 7d oder perm.";
        }

        UUID uuid = target.getUniqueId();
        long now = System.currentTimeMillis();
        long expiresAt = parsedDuration.permanent() ? 0L : now + parsedDuration.durationMillis();

        String ipAddress = null;
        if (target.isOnline() && target.getPlayer() != null) {
            ipAddress = target.getPlayer().getAddress() != null ? 
                target.getPlayer().getAddress().getAddress().getHostAddress() : null;
        }

        if (ipAddress == null) {
            return "Konnte IP-Adresse nicht ermitteln. Der Spieler muss online sein.";
        }

        String ipKey = "ip_" + ipAddress.replace(".", "_");
        bansConfig.set(ipKey + ".name", targetName);
        bansConfig.set(ipKey + ".bannedBy", bannedBy);
        bansConfig.set(ipKey + ".reason", reason);
        bansConfig.set(ipKey + ".createdAt", now);
        bansConfig.set(ipKey + ".expiresAt", expiresAt);
        bansConfig.set(ipKey + ".ipAddress", ipAddress);
        bansConfig.set(ipKey + ".isIpBan", true);
        save();

        bansConfig.set(uuid + ".name", targetName);
        bansConfig.set(uuid + ".bannedBy", bannedBy);
        bansConfig.set(uuid + ".reason", reason);
        bansConfig.set(uuid + ".createdAt", now);
        bansConfig.set(uuid + ".expiresAt", expiresAt);
        bansConfig.set(uuid + ".ipAddress", ipAddress);
        bansConfig.set(uuid + ".isIpBan", true);
        save();

        BanRecord record = new BanRecord(uuid, targetName, bannedBy, reason, now, expiresAt, ipAddress, true);
        Player onlineTarget = Bukkit.getPlayerExact(targetName);
        if (onlineTarget != null) {
            onlineTarget.kick(net.kyori.adventure.text.Component.text(createBanScreen(record)));
        }

        plugin.getDiscordWebhookService().sendBan(record.playerName(), record.bannedBy(), record.reason(), 
            record.isPermanent() ? "Permanent" : record.formattedRemainingDuration());
        return null;
    }

    public boolean editBan(UUID uuid, String newReason, Long newExpiresAt) {
        if (!bansConfig.contains(uuid.toString())) {
            return false;
        }

        if (newReason != null) {
            bansConfig.set(uuid + ".reason", newReason);
        }
        if (newExpiresAt != null) {
            bansConfig.set(uuid + ".expiresAt", newExpiresAt);
        }
        save();
        return true;
    }

    public java.util.List<BanRecord> getAllBans() {
        java.util.List<BanRecord> bans = new java.util.ArrayList<>();
        for (String key : bansConfig.getKeys(false)) {
            if (key.startsWith("ip_")) continue;
            
            try {
                UUID uuid = UUID.fromString(key);
                BanRecord record = getActiveBan(uuid);
                if (record != null) {
                    bans.add(record);
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return bans;
    }

    public boolean unban(String targetName, String unbannedBy) {
        String key = findBanKeyByName(targetName);
        if (key == null) {
            return false;
        }

        bansConfig.set(key, null);
        save();
        plugin.getDiscordWebhookService().sendUnban(targetName, unbannedBy);
        return true;
    }

    public BanRecord getActiveBan(UUID uuid) {
        if (!bansConfig.contains(uuid.toString())) {
            return null;
        }

        BanRecord record = new BanRecord(
                uuid,
                bansConfig.getString(uuid + ".name", "Unbekannt"),
                bansConfig.getString(uuid + ".bannedBy", "Konsole"),
                bansConfig.getString(uuid + ".reason", "Kein Grund angegeben"),
                bansConfig.getLong(uuid + ".createdAt"),
                bansConfig.getLong(uuid + ".expiresAt"),
                bansConfig.getString(uuid + ".ipAddress"),
                bansConfig.getBoolean(uuid + ".isIpBan", false)
        );

        if (record.isExpired()) {
            bansConfig.set(uuid.toString(), null);
            save();
            return null;
        }
        return record;
    }

    public String createBanScreen(BanRecord record) {
        String lineBreak = "\n";
        return ChatColor.DARK_RED + "" + ChatColor.BOLD + "CrimsonBann" + lineBreak
                + ChatColor.GRAY + "CrimsonBan" + lineBreak + lineBreak
                + ChatColor.RED + "Du wurdest vom Server gebannt." + lineBreak + lineBreak
                + ChatColor.DARK_GRAY + "Gebannt von: " + ChatColor.WHITE + record.bannedBy() + lineBreak
                + ChatColor.DARK_GRAY + "Grund: " + ChatColor.WHITE + record.reason() + lineBreak
                + ChatColor.DARK_GRAY + "Dauer: " + ChatColor.WHITE + (record.isPermanent() ? "Permanent" : record.formattedRemainingDuration()) + lineBreak
                + lineBreak
                + ChatColor.GRAY + "Entbannung per Ticket:" + lineBreak
                + ChatColor.RED + "discord.gg/reloadedsmp";
    }

    public String prefix(String message) {
        return ChatColor.DARK_RED + "" + ChatColor.BOLD + "CrimsonBann"
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.GRAY + message;
    }

    private void save() {
        try {
            bansConfig.save(bansFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("bans.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    private String findBanKeyByName(String playerName) {
        for (String key : bansConfig.getKeys(false)) {
            String storedName = bansConfig.getString(key + ".name", "");
            if (storedName.equalsIgnoreCase(playerName)) {
                return key;
            }
        }
        return null;
    }

    public static ParsedDuration parseDurationStatic(String input) {
        String normalized = input.toLowerCase();
        if (normalized.equals("perm") || normalized.equals("permanent")) {
            return new ParsedDuration(true, true, 0L);
        }

        if (normalized.length() < 2) {
            return new ParsedDuration(false, false, 0L);
        }

        char unit = normalized.charAt(normalized.length() - 1);
        String numberPart = normalized.substring(0, normalized.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(numberPart);
        } catch (NumberFormatException exception) {
            return new ParsedDuration(false, false, 0L);
        }

        long multiplier = switch (unit) {
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            case 'd' -> 86_400_000L;
            default -> -1L;
        };

        if (amount <= 0 || multiplier < 0) {
            return new ParsedDuration(false, false, 0L);
        }
        return new ParsedDuration(true, false, amount * multiplier);
    }

    private ParsedDuration parseDuration(String input) {
        return parseDurationStatic(input);
    }

    public DiscordWebhookService getDiscordWebhookService() {
        return plugin.getDiscordWebhookService();
    }

    public void banConsole(String playerName, String bannedBy, String reason) {
        String uuid = "console_" + System.currentTimeMillis();
        long now = System.currentTimeMillis();
        
        bansConfig.set(uuid + ".playerName", playerName);
        bansConfig.set(uuid + ".bannedBy", bannedBy);
        bansConfig.set(uuid + ".reason", reason);
        bansConfig.set(uuid + ".bannedAt", now);
        bansConfig.set(uuid + ".expiresAt", 0L);
        bansConfig.set(uuid + ".isPermanent", true);
        bansConfig.set(uuid + ".isIpBan", false);
        
        save();
        
        plugin.getDiscordWebhookService().sendBan(playerName, bannedBy, reason, "Permanent");
        
        plugin.getLogger().info("Console " + playerName + " wurde von " + bannedBy + " gebannt: " + reason);
    }

    public void unbanConsole(String playerName) {
        String foundKey = null;
        for (String key : bansConfig.getKeys(false)) {
            if (key.startsWith("console_") && playerName.equals(bansConfig.getString(key + ".playerName"))) {
                foundKey = key;
                break;
            }
        }
        
        if (foundKey != null) {
            bansConfig.set(foundKey, null);
            save();
            
            plugin.getDiscordWebhookService().sendUnban(playerName, "System");
            plugin.getLogger().info("Console " + playerName + " wurde entbannt");
        }
    }

    public record ParsedDuration(boolean valid, boolean permanent, long durationMillis) {
    }
}
