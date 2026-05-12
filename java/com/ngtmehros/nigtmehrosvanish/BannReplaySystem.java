package com.ngtmehros.nigtmehrosvanish;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BannReplaySystem implements Listener {

    private final NigtmehrosBanPlugin plugin;
    private final Map<UUID, List<ReplayFrame>> recordingData = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> recordingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> recordingStart = new ConcurrentHashMap<>();
    private final int RECORDING_INTERVAL = 100; // 5 Ticks = 250ms
    private final int MAX_RECORDING_TIME = 30000; // 30 Sekunden

    public BannReplaySystem(NigtmehrosBanPlugin plugin) {
        this.plugin = plugin;
        createReplayFolder();
    }

    private void createReplayFolder() {
        Path replayFolder = Paths.get(plugin.getDataFolder().getPath(), "replays");
        try {
            if (!Files.exists(replayFolder)) {
                Files.createDirectories(replayFolder);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Replay-Ordner nicht erstellen: " + e.getMessage());
        }
    }

    public void startRecording(UUID playerUuid) {
        if (isRecording(playerUuid)) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        recordingData.put(playerUuid, new ArrayList<>());
        recordingStart.put(playerUuid, System.currentTimeMillis());

        // Starte Recording Task
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            recordFrame(playerUuid);
        }, 0L, RECORDING_INTERVAL);

        recordingTasks.put(playerUuid, task);

        // Stoppe automatisch nach MAX_RECORDING_TIME
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            stopRecording(playerUuid);
        }, MAX_RECORDING_TIME / 50);
    }

    public void stopRecording(UUID playerUuid) {
        if (!isRecording(playerUuid)) {
            return;
        }

        // Task stoppen
        BukkitTask task = recordingTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }

        // Replay speichern
        List<ReplayFrame> frames = recordingData.remove(playerUuid);
        Long startTime = recordingStart.remove(playerUuid);

        if (frames != null && !frames.isEmpty() && startTime != null) {
            saveReplay(playerUuid, frames, startTime);
        }
    }

    private void recordFrame(UUID playerUuid) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            stopRecording(playerUuid);
            return;
        }

        List<ReplayFrame> frames = recordingData.get(playerUuid);
        if (frames == null) {
            return;
        }

        Location loc = player.getLocation();
        ReplayFrame frame = new ReplayFrame(
            System.currentTimeMillis(),
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getYaw(), loc.getPitch(),
            player.isSneaking(), player.isSprinting(),
            player.getHealth(), player.getFoodLevel()
        );

        frames.add(frame);
    }

    private void saveReplay(UUID playerUuid, List<ReplayFrame> frames, long startTime) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "replay_" + playerUuid.toString().substring(0, 8) + "_" + timestamp + ".json";
        Path filePath = Paths.get(plugin.getDataFolder().getPath(), "replays", fileName);

        try {
            ReplayData replayData = new ReplayData(
                playerUuid.toString(),
                Bukkit.getOfflinePlayer(playerUuid).getName(),
                startTime,
                System.currentTimeMillis(),
                frames
            );

            Files.write(filePath, replayData.toJson().getBytes());
            plugin.getLogger().info("Replay gespeichert: " + fileName);

            // Discord Webhook senden
            plugin.getDiscordWebhookService().sendReplaySaved(
                Bukkit.getOfflinePlayer(playerUuid).getName(),
                fileName,
                frames.size()
            );

        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Replay nicht speichern: " + e.getMessage());
        }
    }

    public String getReplayData(UUID playerUuid) {
        // Finde die neueste Replay-Datei für diesen Spieler
        Path replayFolder = Paths.get(plugin.getDataFolder().getPath(), "replays");
        try {
            Optional<Path> latestReplay = Files.list(replayFolder)
                    .filter(path -> path.toString().contains(playerUuid.toString().substring(0, 8)))
                    .max(Comparator.comparingLong(path -> path.toFile().lastModified()));

            if (latestReplay.isPresent()) {
                return Files.readString(latestReplay.get());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Lesen von Replay-Daten: " + e.getMessage());
        }
        return null;
    }

    public boolean isRecording(UUID playerUuid) {
        return recordingTasks.containsKey(playerUuid);
    }

    public List<String> getAllReplays() {
        List<String> replays = new ArrayList<>();
        Path replayFolder = Paths.get(plugin.getDataFolder().getPath(), "replays");
        try {
            Files.list(replayFolder)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> replays.add(path.getFileName().toString()));
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Auflisten der Replays: " + e.getMessage());
        }
        return replays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Stoppe Recording wenn Spieler leaves
        stopRecording(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Nur Recording wenn Spieler sich wirklich bewegt (nur Head-Bewegungen ignorieren)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        // Recording wird im Task gemacht, hier nur prüfen ob aktiv
        UUID uuid = event.getPlayer().getUniqueId();
        if (isRecording(uuid)) {
            // Bewegung wird im nächsten Frame aufgenommen
        }
    }

    // Data Classes
    public static class ReplayFrame {
        public final long timestamp;
        public final double x, y, z;
        public final float yaw, pitch;
        public final boolean sneaking, sprinting;
        public final double health;
        public final int foodLevel;

        public ReplayFrame(long timestamp, double x, double y, double z, float yaw, float pitch,
                          boolean sneaking, boolean sprinting, double health, int foodLevel) {
            this.timestamp = timestamp;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.sneaking = sneaking;
            this.sprinting = sprinting;
            this.health = health;
            this.foodLevel = foodLevel;
        }
    }

    public static class ReplayData {
        public final String playerUuid;
        public final String playerName;
        public final long startTime;
        public final long endTime;
        public final List<ReplayFrame> frames;

        public ReplayData(String playerUuid, String playerName, long startTime, long endTime, List<ReplayFrame> frames) {
            this.playerUuid = playerUuid;
            this.playerName = playerName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.frames = frames;
        }

        public String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"playerUuid\":\"").append(playerUuid).append("\",");
            json.append("\"playerName\":\"").append(playerName).append("\",");
            json.append("\"startTime\":").append(startTime).append(",");
            json.append("\"endTime\":").append(endTime).append(",");
            json.append("\"frames\":[");
            
            for (int i = 0; i < frames.size(); i++) {
                ReplayFrame frame = frames.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"timestamp\":").append(frame.timestamp).append(",");
                json.append("\"x\":").append(frame.x).append(",");
                json.append("\"y\":").append(frame.y).append(",");
                json.append("\"z\":").append(frame.z).append(",");
                json.append("\"yaw\":").append(frame.yaw).append(",");
                json.append("\"pitch\":").append(frame.pitch).append(",");
                json.append("\"sneaking\":").append(frame.sneaking).append(",");
                json.append("\"sprinting\":").append(frame.sprinting).append(",");
                json.append("\"health\":").append(frame.health).append(",");
                json.append("\"foodLevel\":").append(frame.foodLevel);
                json.append("}");
            }
            
            json.append("]}");
            return json.toString();
        }
    }
}
