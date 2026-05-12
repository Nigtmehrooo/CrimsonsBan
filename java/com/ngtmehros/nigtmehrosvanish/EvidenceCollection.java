package com.ngtmehros.nigtmehrosvanish;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EvidenceCollection implements Listener {

    private final NigtmehrosBanPlugin plugin;
    private final Map<UUID, List<EvidenceEntry>> playerEvidence = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    private final int MAX_EVIDENCE_PER_PLAYER = 1000;
    private final int EVIDENCE_RETENTION_DAYS = 30;

    public EvidenceCollection(NigtmehrosBanPlugin plugin) {
        this.plugin = plugin;
        createEvidenceFolder();
        cleanupOldEvidence();
    }

    private void createEvidenceFolder() {
        Path evidenceFolder = Paths.get(plugin.getDataFolder().getPath(), "evidence");
        try {
            if (!Files.exists(evidenceFolder)) {
                Files.createDirectories(evidenceFolder);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Evidence-Ordner nicht erstellen: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        sessionStart.put(uuid, System.currentTimeMillis());
        playerEvidence.putIfAbsent(uuid, new ArrayList<>());
        
        // Join-Evidence sammeln
        addEvidence(uuid, EvidenceType.JOIN, "Player joined server", 
                  "IP: " + player.getAddress().getAddress().getHostAddress());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Quit-Evidence sammeln
        addEvidence(uuid, EvidenceType.QUIT, "Player quit server", 
                  "Session duration: " + (System.currentTimeMillis() - sessionStart.getOrDefault(uuid, 0L)) + "ms");
        
        sessionStart.remove(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Chat-Evidence sammeln
        addEvidence(uuid, EvidenceType.CHAT, event.getMessage(), 
                  "Location: " + formatLocation(player.getLocation()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String command = event.getMessage();
        
        // Command-Evidence sammeln (nur bei verdächtigen Commands)
        if (isSuspiciousCommand(command)) {
            addEvidence(uuid, EvidenceType.COMMAND, command, 
                      "Location: " + formatLocation(player.getLocation()));
        }
    }

    public void addEvidence(UUID playerUuid, EvidenceType type, String content, String metadata) {
        List<EvidenceEntry> evidence = playerEvidence.computeIfAbsent(playerUuid, k -> new ArrayList<>());
        
        EvidenceEntry entry = new EvidenceEntry(
            System.currentTimeMillis(),
            type,
            content,
            metadata,
            Bukkit.getOfflinePlayer(playerUuid).getName()
        );
        
        evidence.add(entry);
        
        // Alte Evidence entfernen wenn zu viele
        if (evidence.size() > MAX_EVIDENCE_PER_PLAYER) {
            evidence.remove(0);
        }
    }

    public void addScreenshotEvidence(UUID playerUuid, String screenshotPath, String reason) {
        addEvidence(playerUuid, EvidenceType.SCREENSHOT, "Screenshot captured: " + screenshotPath, 
                  "Reason: " + reason);
    }

    public void addLocationEvidence(UUID playerUuid, Location location, String reason) {
        addEvidence(playerUuid, EvidenceType.LOCATION, "Location evidence", 
                  "Position: " + formatLocation(location) + " | Reason: " + reason);
    }

    public void addInventoryEvidence(UUID playerUuid, String inventoryData, String reason) {
        addEvidence(playerUuid, EvidenceType.INVENTORY, "Inventory snapshot", 
                  "Data: " + inventoryData + " | Reason: " + reason);
    }

    public EvidenceReport generateEvidenceReport(UUID playerUuid, String reason) {
        List<EvidenceEntry> evidence = playerEvidence.getOrDefault(playerUuid, new ArrayList<>());
        
        // Filtere relevante Evidence (letzte 24 Stunden)
        long cutoff = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        List<EvidenceEntry> relevantEvidence = evidence.stream()
                .filter(entry -> entry.timestamp > cutoff)
                .sorted(Comparator.comparingLong(entry -> entry.timestamp))
                .toList();
        
        return new EvidenceReport(
            Bukkit.getOfflinePlayer(playerUuid).getName(),
            playerUuid.toString(),
            reason,
            relevantEvidence,
            System.currentTimeMillis()
        );
    }

    public void saveEvidenceReport(EvidenceReport report) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "evidence_" + report.playerUuid.substring(0, 8) + "_" + timestamp + ".txt";
        Path filePath = Paths.get(plugin.getDataFolder().getPath(), "evidence", fileName);

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("=== CRIMSONBANN EVIDENCE REPORT ===\n");
            writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Player: " + report.playerName + " (" + report.playerUuid + ")\n");
            writer.write("Reason: " + report.reason + "\n");
            writer.write("Evidence Count: " + report.evidence.size() + "\n");
            writer.write("\n=== EVIDENCE ===\n\n");

            for (EvidenceEntry entry : report.evidence) {
                writer.write("[" + formatTimestamp(entry.timestamp) + "] " + entry.type.name() + "\n");
                writer.write("Content: " + entry.content + "\n");
                writer.write("Metadata: " + entry.metadata + "\n");
                writer.write("\n");
            }

            plugin.getLogger().info("Evidence Report gespeichert: " + fileName);

            // Discord Webhook senden
            plugin.getDiscordWebhookService().sendEvidenceReport(
                report.playerName,
                fileName,
                report.evidence.size()
            );

        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Evidence Report nicht speichern: " + e.getMessage());
        }
    }

    private boolean isSuspiciousCommand(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("/op") || lowerCmd.contains("/deop") ||
               lowerCmd.contains("/ban") || lowerCmd.contains("/kick") ||
               lowerCmd.contains("/stop") || lowerCmd.contains("/reload") ||
               lowerCmd.contains("/whitelist") || lowerCmd.contains("/gamemode");
    }

    private String formatLocation(Location loc) {
        return String.format("%.1f,%.1f,%.1f (%s)", 
            loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
    }

    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(
            new Date(timestamp).toInstant(), 
            java.time.ZoneId.systemDefault()
        ).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private void cleanupOldEvidence() {
        long cutoff = System.currentTimeMillis() - (EVIDENCE_RETENTION_DAYS * 24 * 60 * 60 * 1000L);
        
        playerEvidence.forEach((uuid, evidence) -> {
            evidence.removeIf(entry -> entry.timestamp < cutoff);
            if (evidence.isEmpty()) {
                playerEvidence.remove(uuid);
            }
        });

        // Alte Evidence-Dateien löschen
        Path evidenceFolder = Paths.get(plugin.getDataFolder().getPath(), "evidence");
        try {
            if (Files.exists(evidenceFolder)) {
                Files.list(evidenceFolder)
                        .filter(path -> path.toFile().lastModified() < cutoff)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                plugin.getLogger().warning("Konnte alte Evidence-Datei nicht löschen: " + path);
                            }
                        });
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Cleanup alter Evidence: " + e.getMessage());
        }
    }

    public List<EvidenceEntry> getPlayerEvidence(UUID playerUuid) {
        return playerEvidence.getOrDefault(playerUuid, new ArrayList<>());
    }

    public List<String> getAllEvidenceReports() {
        List<String> reports = new ArrayList<>();
        Path evidenceFolder = Paths.get(plugin.getDataFolder().getPath(), "evidence");
        try {
            Files.list(evidenceFolder)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .forEach(path -> reports.add(path.getFileName().toString()));
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Auflisten der Evidence Reports: " + e.getMessage());
        }
        return reports;
    }

    // Data Classes
    public enum EvidenceType {
        JOIN, QUIT, CHAT, COMMAND, SCREENSHOT, LOCATION, INVENTORY, CUSTOM
    }

    public static class EvidenceEntry {
        public final long timestamp;
        public final EvidenceType type;
        public final String content;
        public final String metadata;
        public final String playerName;

        public EvidenceEntry(long timestamp, EvidenceType type, String content, String metadata, String playerName) {
            this.timestamp = timestamp;
            this.type = type;
            this.content = content;
            this.metadata = metadata;
            this.playerName = playerName;
        }
    }

    public static class EvidenceReport {
        public final String playerName;
        public final String playerUuid;
        public final String reason;
        public final List<EvidenceEntry> evidence;
        public final long generatedAt;

        public EvidenceReport(String playerName, String playerUuid, String reason, 
                            List<EvidenceEntry> evidence, long generatedAt) {
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.reason = reason;
            this.evidence = evidence;
            this.generatedAt = generatedAt;
        }
    }
}
