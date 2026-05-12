package com.ngtmehros.nigtmehrosvanish;

import com.ngtmehros.nigtmehrosvanish.BanRecord;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public final class BanListener implements Listener {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public BanListener(BanManager banManager) {
        this.banManager = banManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        BanRecord record = banManager.getActiveBan(event.getUniqueId());
        if (record == null) {
            return;
        }

        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banManager.createBanScreen(record));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Show admin display to admins with permission
        if (player.hasPermission("crimisonbann.admin")) {
            Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("CrimsonBann"), () -> {
                showRecentBansToAdmin(player);
            }, 20L);
        }
    }

    private void showRecentBansToAdmin(Player player) {
        var bans = banManager.getAllBans();
        if (bans.isEmpty()) {
            return;
        }

        // Show the most recent ban in action bar
        BanRecord mostRecent = bans.get(0);
        String duration = mostRecent.isPermanent() ? "Permanent" : mostRecent.formattedRemainingDuration();
        player.sendActionBar(MINI_MESSAGE.deserialize("<red>[CrimsonBann]</red> <white>...</white> <red>[" + duration + "]</red>"));
    }
}
