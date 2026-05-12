package com.ngtmehros.nigtmehrosvanish;

import com.ngtmehros.nigtmehrosvanish.BanRecord;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;

public final class DiscordWebhookService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z");

    private final NigtmehrosBanPlugin plugin;
    private final HttpClient httpClient;

    public DiscordWebhookService(NigtmehrosBanPlugin plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void sendBan(BanRecord record) {
        String payload = "{"
                + "\"username\":\"" + escape(plugin.getConfig().getString("discord.username", "CrimsonBann")) + "\","
                + "\"avatar_url\":\"" + escape(plugin.getConfig().getString("discord.avatar-url", "")) + "\","
                + "\"embeds\":[{"
                + "\"title\":\"" + escape(plugin.getConfig().getString("discord.embeds.ban.title", "🔨 Spieler gebannt")) + "\","
                + "\"description\":\"" + escape(plugin.getConfig().getString("discord.embeds.ban.description", "Ein Spieler wurde vom Server gebannt.")) + "\","
                + "\"color\":" + plugin.getConfig().getInt("discord.embeds.ban.color", 14495300) + ","
                + "\"thumbnail\":{\"url\":\"" + escape(plugin.getConfig().getString("discord.embeds.thumbnail-url", "")) + "\"},"
                + "\"footer\":{\"text\":\"" + escape("CrimsonBann | " + nowString()) + "\",\"icon_url\":\"" + escape(plugin.getConfig().getString("discord.avatar-url", "")) + "\"},"
                + "\"fields\":["
                + field("👤 Spieler", record.playerName(), true) + ","
                + field("🔨 Moderator", record.bannedBy(), true) + ","
                + field("⏰ Dauer", formatDuration(record), true) + ","
                + field("📝 Grund", record.reason(), false)
                + "]"
                + "}]"
                + "}";
        sendConfigured(payload);
    }

    public void sendBan(String bannedPlayer, String bannedBy, String reason, String duration) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.ban", "14495300"); // Rot
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{"
                + "\"title\":\"🔒 IP-Bann ausgeführt\"," +
                "\"description\":\"**👤 Spieler:** " + escapeJson(bannedPlayer) + "\\n**🔨 Moderator:** " + escapeJson(bannedBy) + "\\n**📝 Grund:** " + escapeJson(reason) + "\\n**⏰ Dauer:** " + escapeJson(duration) + "\"," +
                "\"color\":" + color + ","
                + "\"thumbnail\":{\"url\":\"" + escape(plugin.getConfig().getString("discord.embeds.thumbnail-url", "")) + "\"},"
                + "\"footer\":{\"text\":\"" + escape("CrimsonBann | " + nowString()) + "\",\"icon_url\":\"" + escape(plugin.getConfig().getString("discord.avatar-url", "")) + "\"},"
                + "\"fields\":["
                + field("🌐 IP-Adresse", "Nicht verfügbar", true) + ","
                + field("🔨 Art", "IP-Bann", true) + ","
                + field("⏰ Dauer", duration, true) + ","
                + field("📝 Grund", reason, false)
                + "]"
                + "}]}" ;
        
        sendWebhook(webhookUrl, jsonPayload);
    }

    public void sendUnban(String playerName, String unbannedBy) {
        String payload = "{"
                + "\"username\":\"" + escape(plugin.getConfig().getString("discord.username", "CrimsonBann")) + "\","
                + "\"avatar_url\":\"" + escape(plugin.getConfig().getString("discord.avatar-url", "")) + "\","
                + "\"embeds\":[{"
                + "\"title\":\"" + escape(plugin.getConfig().getString("discord.embeds.unban.title", "✅ Spieler entbannt")) + "\","
                + "\"description\":\"" + escape(plugin.getConfig().getString("discord.embeds.unban.description", "Ein Spieler wurde wieder freigegeben.")) + "\","
                + "\"color\":" + plugin.getConfig().getInt("discord.embeds.unban.color", 5763719) + ","
                + "\"footer\":{\"text\":\"" + escape("CrimsonBann | " + nowString()) + "\"},"
                + "\"fields\":["
                + field("Spieler", playerName, true) + ","
                + field("Moderator", unbannedBy, true)
                + "]"
                + "}]"
                + "}";
        sendConfigured(payload);
    }

    public void sendMultiAccountAlert(String playerName, String playerUuid, String ip, String bannedAccount, String banReason) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.multi-account", "16776960"); // Gelb
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{" +
                "\"title\":\"⚠️ Multi-Account Detection\"," +
                "\"description\":\"**Neuer Account:** " + escapeJson(playerName) + "\\n**UUID:** " + escapeJson(playerUuid) + "\\n**IP-Adresse:** " + escapeJson(ip) + "\\n**Gebannter Account:** " + escapeJson(bannedAccount) + "\\n**Bann-Grund:** " + escapeJson(banReason) + "\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.now().toString() + "\"" +
                "}]}" ;
        
        sendWebhook(webhookUrl, jsonPayload);
    }

    public void sendTooManyAccountsAlert(String playerName, String playerUuid, String ip, int accountCount) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.too-many-accounts", "16711680"); // Rot
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{" +
                "\"title\":\"🚫 Zu viele Accounts\"," +
                "\"description\":\"**Spieler:** " + escapeJson(playerName) + "\\n**UUID:** " + escapeJson(playerUuid) + "\\n**IP-Adresse:** " + escapeJson(ip) + "\\n**Account-Anzahl:** " + accountCount + "\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.now().toString() + "\"" +
                "}]}" ;
        
        sendWebhook(webhookUrl, jsonPayload);
    }

    public void sendReplaySaved(String playerName, String replayFile, int frameCount) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.replay", "16711680"); // Rot
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{" +
                "\"title\":\"📹 Replay gespeichert\"," +
                "\"description\":\"**Spieler:** " + escapeJson(playerName) + "\\n**Replay-Datei:** " + escapeJson(replayFile) + "\\n**Frame-Anzahl:** " + frameCount + "\\n**Dauer:** ~" + (frameCount * 250 / 1000) + " Sekunden\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.now().toString() + "\"" +
                "}]}" ;
        
        sendWebhook(webhookUrl, jsonPayload);
    }

    public void sendEvidenceReport(String playerName, String reportFile, int evidenceCount) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.evidence", "16711680"); // Rot
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{" +
                "\"title\":\"📋 Evidence Report erstellt\"," +
                "\"description\":\"**Spieler:** " + escapeJson(playerName) + "\\n**Report-Datei:** " + escapeJson(reportFile) + "\\n**Evidence-Anzahl:** " + evidenceCount + "\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.now().toString() + "\"" +
                "}]}" ;
        
        sendWebhook(webhookUrl, jsonPayload);
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false);
    }

    private void sendWebhook(String webhookUrl, String payload) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                plugin.getLogger().warning("Webhook konnte nicht gesendet werden. HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            plugin.getLogger().warning("Webhook Fehler: " + exception.getMessage());
        }
    }

    private void sendConfigured(String payload) {
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;
        sendWebhook(webhookUrl, payload);
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String nowString() {
        return TIME_FORMAT.format(ZonedDateTime.now());
    }

    private String field(String name, String value, boolean inline) {
        return "{\"name\":\"" + escape(name) + "\",\"value\":\"" + escape(value) + "\",\"inline\":" + inline + "}";
    }

    private String formatDuration(BanRecord record) {
        return record.isPermanent() ? "Permanent" : record.formattedRemainingDuration();
    }

    public void sendEvidenceEmbed(String playerName, String evidenceType, String description, String details) {
        if (!isEnabled()) return;
        
        String webhookUrl = plugin.getConfig().getString("discord.webhook-url");
        if (webhookUrl == null || webhookUrl.equals("DEINE_WEBHOOK_URL_HIER")) return;

        String username = plugin.getConfig().getString("discord.username", "CrimsonBann");
        String avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        String color = plugin.getConfig().getString("discord.colors.evidence", "16711680"); // Rot
        
        String jsonPayload = "{\"username\":\"" + escapeJson(username) + "\"," +
                (avatarUrl.isEmpty() ? "" : "\"avatar_url\":\"" + escapeJson(avatarUrl) + "\",") +
                "\"embeds\":[{" +
                "\"title\":\"📋 Evidence Collection\"," +
                "\"description\":\"**Spieler:** " + escapeJson(playerName) + "\\n**Typ:** " + escapeJson(evidenceType) + "\\n**Beschreibung:** " + escapeJson(description) + "\\n**Details:** " + escapeJson(details) + "\"," +
                "\"color\":" + color + "," +
                "\"timestamp\":\"" + Instant.now().toString() + "\"" +
                "}]}";
        
        sendWebhook(webhookUrl, jsonPayload);
    }
}
