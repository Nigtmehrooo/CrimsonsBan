package com.nigtmehros.nigtmehrosban;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.nigtmehros.nigtmehrosban.commands.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public final class NigtmehrosBanPlugin extends JavaPlugin {

    private BanManager banManager;
    private DiscordWebhookService discordWebhookService;
    private BannReplaySystem replaySystem;
    private EvidenceCollection evidenceCollection;
    private MultiAccountDetection multiAccountDetection;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("CrimsonBann wurde aktiviert!");
        
        // Konfiguration speichern
        saveDefaultConfig();
        
        // Manager initialisieren
        banManager = new BanManager(this);
        discordWebhookService = new DiscordWebhookService(this);
        replaySystem = new BannReplaySystem(this);
        evidenceCollection = new EvidenceCollection(this);
        multiAccountDetection = new MultiAccountDetection(this, banManager);
        
        // Commands registrieren
        Objects.requireNonNull(getCommand("ban")).setExecutor(new BanCommand(banManager));
        Objects.requireNonNull(getCommand("tempban")).setExecutor(new TempBanCommand(banManager));
        Objects.requireNonNull(getCommand("unban")).setExecutor(new UnbanCommand(banManager));
        Objects.requireNonNull(getCommand("ipban")).setExecutor(new IPBanCommand(banManager));
        BanListGUI banListGUI = new BanListGUI(banManager);
Objects.requireNonNull(getCommand("bannlist")).setExecutor(new BannlistCommand(banManager, banListGUI));
        Objects.requireNonNull(getCommand("editbanreason")).setExecutor(new EditBanReasonCommand(banManager));
        Objects.requireNonNull(getCommand("editbantime")).setExecutor(new EditBanTimeCommand(banManager));
        Objects.requireNonNull(getCommand("evidence")).setExecutor(new EvidenceCommand(banManager, evidenceCollection, replaySystem, multiAccountDetection));
        
        // Listener registrieren
        Bukkit.getPluginManager().registerEvents(new BanListener(banManager), this);
        Bukkit.getPluginManager().registerEvents(new BanGUIListener(banManager, banListGUI), this);
        Bukkit.getPluginManager().registerEvents(replaySystem, this);
        Bukkit.getPluginManager().registerEvents(evidenceCollection, this);
        Bukkit.getPluginManager().registerEvents(multiAccountDetection, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("CrimsonBann wurde deaktiviert!");
        
        // Alle Replay-Aufnahmen stoppen
        if (replaySystem != null) {
            // Cleanup für alle laufenden Recordings
        }
    }
    
    public BanManager getBanManager() {
        return banManager;
    }
    
    public DiscordWebhookService getDiscordWebhookService() {
        return discordWebhookService;
    }
    
    public BannReplaySystem getReplaySystem() {
        return replaySystem;
    }
    
    public EvidenceCollection getEvidenceCollection() {
        return evidenceCollection;
    }
    
    public MultiAccountDetection getMultiAccountDetection() {
        return multiAccountDetection;
    }
    
    private void createBansFile() {
        File file = new File(getDataFolder(), "bans.yml");
        if (file.exists()) {
            return;
        }

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Plugin-Ordner konnte nicht erstellt werden.");
            return;
        }

        try {
            if (!file.createNewFile()) {
                getLogger().warning("bans.yml konnte nicht erstellt werden.");
            }
        } catch (IOException exception) {
            getLogger().warning("Fehler beim Erstellen von bans.yml: " + exception.getMessage());
        }
    }
}
