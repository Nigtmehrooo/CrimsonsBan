package com.nigtmehros.nigtmehrosban.commands;

import com.nigtmehros.nigtmehrosban.BanManager;
import com.nigtmehros.nigtmehrosban.BanRecord;
import com.nigtmehros.nigtmehrosban.DiscordWebhookService;
import com.nigtmehros.nigtmehrosban.EvidenceCollection;
import com.nigtmehros.nigtmehrosban.BannReplaySystem;
import com.nigtmehros.nigtmehrosban.MultiAccountDetection;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EvidenceCommand implements CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;
    private final EvidenceCollection evidenceCollection;
    private final BannReplaySystem replaySystem;
    private final MultiAccountDetection multiAccountDetection;
    private final DiscordWebhookService discordWebhookService;

    public EvidenceCommand(BanManager banManager, EvidenceCollection evidenceCollection, 
                          BannReplaySystem replaySystem, MultiAccountDetection multiAccountDetection) {
        this.banManager = banManager;
        this.evidenceCollection = evidenceCollection;
        this.replaySystem = replaySystem;
        this.multiAccountDetection = multiAccountDetection;
        this.discordWebhookService = banManager.getDiscordWebhookService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.evidence")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /evidence <spieler> <action></red>"));
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Actions: report, replay, accounts, screenshot, location, inventory</gray>"));
            return true;
        }

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler nicht gefunden.</red>"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Bitte gib eine Action an.</red>"));
            return true;
        }

        String action = args[1].toLowerCase();
        UUID targetUuid = target.getUniqueId();

        switch (action) {
            case "report" -> createEvidenceReport(sender, targetUuid, targetName);
            case "replay" -> startReplayRecording(sender, targetUuid, targetName);
            case "accounts" -> showLinkedAccounts(sender, targetUuid, targetName);
            case "screenshot" -> takeScreenshotEvidence(sender, targetUuid, targetName);
            case "location" -> captureLocationEvidence(sender, targetUuid, targetName);
            case "inventory" -> captureInventoryEvidence(sender, targetUuid, targetName);
            default -> {
                sender.sendMessage(MINI_MESSAGE.deserialize("<red>Unbekannte Action: " + action + "</red>"));
                sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Verfügbare Actions: report, replay, accounts, screenshot, location, inventory</gray>"));
            }
        }

        return true;
    }

    private void createEvidenceReport(CommandSender sender, UUID targetUuid, String targetName) {
        EvidenceCollection.EvidenceReport report = evidenceCollection.generateEvidenceReport(targetUuid, "Manual evidence collection");
        evidenceCollection.saveEvidenceReport(report);
        
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Evidence Report für <yellow>" + targetName + "</yellow> wurde erstellt.</green>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Evidence Count: " + report.evidence.size() + "</gray>"));
        
        String details = "Gesammelte Evidence: " + report.evidence.size() + " Einträge";
        discordWebhookService.sendEvidenceEmbed(targetName, "Evidence Report", "Manual Evidence Collection", details);
    }

    private void startReplayRecording(CommandSender sender, UUID targetUuid, String targetName) {
        if (replaySystem.isRecording(targetUuid)) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Replay für <yellow>" + targetName + "</yellow> läuft bereits.</red>"));
            return;
        }

        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler <yellow>" + targetName + "</yellow> ist nicht online.</red>"));
            return;
        }

        replaySystem.startRecording(targetUuid);
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Replay-Recording für <yellow>" + targetName + "</yellow> gestartet.</green>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Dauer: 30 Sekunden</gray>"));
        
        String details = "Dauer: 30 Sekunden\nTyp: Bewegungsaufnahme";
        discordWebhookService.sendEvidenceEmbed(targetName, "Replay Recording", "Replay-Aufnahme gestartet", details);
    }

    private void showLinkedAccounts(CommandSender sender, UUID targetUuid, String targetName) {
        java.util.Set<UUID> linkedAccounts = multiAccountDetection.getLinkedAccounts(targetUuid);
        String playerIP = multiAccountDetection.getPlayerIP(targetUuid);

        sender.sendMessage(MINI_MESSAGE.deserialize("<yellow>=== Multi-Account Info für " + targetName + " ===</yellow>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>IP-Adresse: " + (playerIP != null ? playerIP : "Unbekannt") + "</gray>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Verknüpfte Accounts: " + linkedAccounts.size() + "</gray>"));

        if (!linkedAccounts.isEmpty()) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Accounts:</gray>"));
            for (UUID linkedUuid : linkedAccounts) {
                OfflinePlayer linkedPlayer = Bukkit.getOfflinePlayer(linkedUuid);
                BanRecord banRecord = banManager.getActiveBan(linkedUuid);
                String status = banRecord != null ? "<red>✗ Gebannt</red>" : "<a>✅ OK</a>";
                sender.sendMessage(MINI_MESSAGE.deserialize("<gray>- " + linkedPlayer.getName() + " " + status + "</gray>"));
            }
        }
        
        String details = "IP: " + (playerIP != null ? playerIP : "Unbekannt") + 
                        "\nVerknüpfte Accounts: " + linkedAccounts.size();
        discordWebhookService.sendEvidenceEmbed(targetName, "Multi-Account Check", "Multi-Account-Informationen", details);
    }

    private void takeScreenshotEvidence(CommandSender sender, UUID targetUuid, String targetName) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler <yellow>" + targetName + "</yellow> ist nicht online.</red>"));}

        String screenshotPath = "screenshots/" + targetName + "_" + System.currentTimeMillis() + ".png";
        evidenceCollection.addScreenshotEvidence(targetUuid, screenshotPath, "Manual screenshot by " + sender.getName());
        
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Screenshot-Evidence für <yellow>" + targetName + "</yellow> wurde hinzugefügt.</green>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Pfad: " + screenshotPath + "</gray>"));
        
        String details = "Pfad: " + screenshotPath + "\nTyp: Screenshot";
        discordWebhookService.sendEvidenceEmbed(targetName, "Screenshot", "Screenshot-Evidence aufgenommen", details);
    }

    private void captureLocationEvidence(CommandSender sender, UUID targetUuid, String targetName) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler <yellow>" + targetName + "</yellow> ist nicht online.</red>"));
            return;
        }

        evidenceCollection.addLocationEvidence(targetUuid, target.getLocation(), "Manual location capture by " + sender.getName());
        
        String location = String.format("World: %s, X: %.1f, Y: %.1f, Z: %.1f", 
            target.getWorld().getName(), target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ());
        String details = "Position: " + location + "\nTyp: Standort-Aufnahme";
        discordWebhookService.sendEvidenceEmbed(targetName, "Location", "Standort-Informationen", details);
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Location-Evidence für <yellow>" + targetName + "</yellow> wurde hinzugefügt.</green>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Position: " + String.format("%.1f, %.1f, %.1f (%s)", 
            target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(), 
            target.getLocation().getWorld().getName()) + "</gray>"));
        
        String inventoryData = "Hotbar: " + (target.getInventory().getItemInMainHand() != null ? target.getInventory().getItemInMainHand().getType().name() : "None") + 
                              ", Armor: " + target.getInventory().getArmorContents().length + " Items";
        String inventoryDetails = "Inhalt: " + inventoryData;
        discordWebhookService.sendEvidenceEmbed(targetName, "Inventory", "Inventory-Evidence aufgenommen", inventoryDetails);
    }

    private void captureInventoryEvidence(CommandSender sender, UUID targetUuid, String targetName) {
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler <yellow>" + targetName + "</yellow> ist nicht online.</red>"));
            return;
        }

        String inventoryData = "Main Inventory: " + target.getInventory().getSize() + " slots, " +
                              "Hotbar: " + target.getInventory().getHeldItemSlot() + ", " +
                              "Armor: " + (target.getInventory().getHelmet() != null ? "✓" : "✗");

        evidenceCollection.addInventoryEvidence(targetUuid, inventoryData, "Manual inventory capture by " + sender.getName());
        sender.sendMessage(MINI_MESSAGE.deserialize("<green>Inventory-Evidence für <yellow>" + targetName + "</yellow> wurde hinzugefügt.</green>"));
        sender.sendMessage(MINI_MESSAGE.deserialize("<gray>" + inventoryData + "</gray>"));
    }
}
