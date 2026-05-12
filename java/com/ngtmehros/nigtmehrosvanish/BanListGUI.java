package com.ngtmehros.nigtmehrosvanish;

import com.ngtmehros.nigtmehrosvanish.BanRecord;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanListGUI {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final String INVENTORY_TITLE = "Gebannte Spieler";
    private final BanManager banManager;

    public BanListGUI(BanManager banManager) {
        this.banManager = banManager;
    }

    public void openBanList(Player player) {
        List<BanRecord> bans = banManager.getAllBans();
        int size = Math.min(54, (bans.size() / 9 + 1) * 9);
        Inventory inventory = Bukkit.createInventory(null, size, ChatColor.RED + INVENTORY_TITLE);

        for (int i = 0; i < bans.size() && i < 54; i++) {
            BanRecord record = bans.get(i);
            ItemStack item = createPlayerHead(record);
            inventory.setItem(i, item);
        }

        player.openInventory(inventory);
    }

    public void openBanDetails(Player player, BanRecord record) {
        Inventory inventory = Bukkit.createInventory(null, 45, ChatColor.RED + "Bann Details: " + record.playerName());

        // Player head
        ItemStack headItem = createPlayerHead(record);
        inventory.setItem(13, headItem);

        // Info items
        ItemStack infoItem = createInfoItem(record);
        inventory.setItem(22, infoItem);

        // Action buttons
        if (!record.isExpired()) {
            ItemStack unbanItem = createUnbanButton();
            inventory.setItem(29, unbanItem);

            ItemStack editReasonItem = createEditReasonButton();
            inventory.setItem(31, editReasonItem);

            ItemStack editTimeItem = createEditTimeButton();
            inventory.setItem(33, editTimeItem);
        }

        // Back button
        ItemStack backItem = createBackButton();
        inventory.setItem(40, backItem);

        player.openInventory(inventory);
    }

    private ItemStack createPlayerHead(BanRecord record) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(record.uuid());
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        
        if (meta != null) {
            meta.setOwningPlayer(offlinePlayer);
            meta.setDisplayName(ChatColor.RED + record.playerName());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Gebannt am: " + ChatColor.WHITE + record.formattedBanDate());
            lore.add(ChatColor.GRAY + "Dauer: " + ChatColor.WHITE + (record.isPermanent() ? "Permanent" : record.formattedRemainingDuration()));
            lore.add(ChatColor.GRAY + "Grund: " + ChatColor.WHITE + record.reason());
            lore.add(ChatColor.GRAY + "Gebannt von: " + ChatColor.WHITE + record.bannedBy());
            if (record.isIpBan()) {
                lore.add(ChatColor.RED + "IP-Bann: " + ChatColor.WHITE + (record.ipAddress() != null ? record.ipAddress() : "Unbekannt"));
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Klicke für Details");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createInfoItem(BanRecord record) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Bann Informationen");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Spieler: " + ChatColor.WHITE + record.playerName());
            lore.add(ChatColor.GRAY + "UUID: " + ChatColor.WHITE + record.uuid().toString());
            lore.add(ChatColor.GRAY + "Gebannt am: " + ChatColor.WHITE + record.formattedBanDate());
            lore.add(ChatColor.GRAY + "Verfällt am: " + ChatColor.WHITE + record.formattedExpiryDate());
            lore.add(ChatColor.GRAY + "Dauer: " + ChatColor.WHITE + (record.isPermanent() ? "Permanent" : record.formattedRemainingDuration()));
            lore.add(ChatColor.GRAY + "Grund: " + ChatColor.WHITE + record.reason());
            lore.add(ChatColor.GRAY + "Gebannt von: " + ChatColor.WHITE + record.bannedBy());
            if (record.isIpBan()) {
                lore.add(ChatColor.RED + "IP-Bann: " + ChatColor.WHITE + (record.ipAddress() != null ? record.ipAddress() : "Unbekannt"));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createUnbanButton() {
        ItemStack item = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Spieler entbannen");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Klicke um diesen Spieler zu entbannen");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createEditReasonButton() {
        ItemStack item = new ItemStack(Material.YELLOW_WOOL);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Grund ändern");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Klicke um den Bann-Grund zu ändern");
            lore.add(ChatColor.GRAY + "Benutze: /editbanreason <spieler> <neuer grund>");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createEditTimeButton() {
        ItemStack item = new ItemStack(Material.ORANGE_WOOL);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Dauer ändern");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Klicke um die Bann-Dauer zu ändern");
            lore.add(ChatColor.GRAY + "Benutze: /editbantime <spieler> <neue dauer>");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Zurück");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Klicke um zur Liste zurückzukehren");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }

    public void handleClick(Player player, ItemStack clickedItem, BanRecord record) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;
        
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        if (displayName.contains("entbannen")) {
            if (player.hasPermission("crimisonbann.unban")) {
                boolean success = banManager.unban(record.playerName(), player.getName());
                if (success) {
                    player.sendMessage(MINI_MESSAGE.deserialize("<green>Spieler wurde entbannt.</green>"));
                    openBanList(player);
                } else {
                    player.sendMessage(MINI_MESSAGE.deserialize("<red>Fehler beim Entbannen.</red>"));
                }
            } else {
                player.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung.</red>"));
            }
        } else if (displayName.contains("Zurück")) {
            openBanList(player);
        }
    }
}
