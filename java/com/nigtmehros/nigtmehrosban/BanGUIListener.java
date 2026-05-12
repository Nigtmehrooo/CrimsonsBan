package com.nigtmehros.nigtmehrosban;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nigtmehros.nigtmehrosban.BanRecord;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BanGUIListener implements Listener {

    private final BanManager banManager;
    private final BanListGUI banListGUI;
    private final Map<UUID, BanRecord> viewingDetails = new HashMap<>();

    public BanGUIListener(BanManager banManager, BanListGUI banListGUI) {
        this.banManager = banManager;
        this.banListGUI = banListGUI;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (!title.contains("Gebannte Spieler") && !title.contains("Bann Details")) return;
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (title.contains("Gebannte Spieler")) {
            // Handle main ban list clicks
            handleBanListClick(player, clickedItem);
        } else if (title.contains("Bann Details")) {
            // Handle ban details clicks
            handleDetailsClick(player, clickedItem, itemName);
        }
    }

    private void handleBanListClick(Player player, ItemStack clickedItem) {
        if (clickedItem.getType() != org.bukkit.Material.PLAYER_HEAD) return;
        
        // Extract player name from the item
        String displayName = clickedItem.getItemMeta().getDisplayName();
        if (displayName == null) return;
        
        String playerName = ChatColor.stripColor(displayName);
        
        // Find the ban record
        for (BanRecord record : banManager.getAllBans()) {
            if (record.playerName().equalsIgnoreCase(playerName)) {
                viewingDetails.put(player.getUniqueId(), record);
                banListGUI.openBanDetails(player, record);
                return;
            }
        }
    }

    private void handleDetailsClick(Player player, ItemStack clickedItem, String itemName) {
        if (itemName == null) return;
        
        BanRecord record = viewingDetails.get(player.getUniqueId());
        if (record == null) {
            banListGUI.openBanList(player);
            return;
        }
        
        // Handle button clicks
        if (itemName.contains("entbannen")) {
            if (player.hasPermission("crimisonbann.unban")) {
                boolean success = banManager.unban(record.playerName(), player.getName());
                if (success) {
                    player.sendMessage(ChatColor.GREEN + "Spieler wurde entbannt.");
                    viewingDetails.remove(player.getUniqueId());
                    banListGUI.openBanList(player);
                } else {
                    player.sendMessage(ChatColor.RED + "Fehler beim Entbannen.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Du hast keine Berechtigung.");
            }
        } else if (itemName.contains("Zurück")) {
            viewingDetails.remove(player.getUniqueId());
            banListGUI.openBanList(player);
        } else if (itemName.contains("Grund ändern")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Benutze: /editbanreason " + record.playerName() + " <neuer grund>");
        } else if (itemName.contains("Dauer ändern")) {
            player.closeInventory();
            player.sendMessage(ChatColor.YELLOW + "Benutze: /editbantime " + record.playerName() + " <neue dauer>");
        }
    }

    public void cleanup(Player player) {
        viewingDetails.remove(player.getUniqueId());
    }
}
