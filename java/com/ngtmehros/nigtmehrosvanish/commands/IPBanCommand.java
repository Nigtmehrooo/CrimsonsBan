package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

public class IPBanCommand implements CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public IPBanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.ipban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /ipban <spieler> <dauer|perm> <grund></red>"));
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Dauer-Beispiele: 30m, 12h, 7d oder perm</gray>"));
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        String bannedBy = sender instanceof Player player ? player.getName() : "Konsole";
        String result = banManager.ipBan(targetName, bannedBy, duration, reason);

        if (result != null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>" + result + "</red>"));
        } else {
            String displayDuration = duration.equals("perm") || duration.equals("permanent") ? "Permanent" : duration;
            String message = "<green>IP von <yellow>" + targetName + "</yellow> wurde für <yellow>" + displayDuration + "</yellow> gebannt.</green>";
            sender.sendMessage(MINI_MESSAGE.deserialize(message));
            
            // Offline-Spieler kicken falls online
            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null) {
                onlineTarget.kick(net.kyori.adventure.text.Component.text("Du wurdest gebannt! Grund: " + reason));
            }
            
            // Show admin display
            showAdminDisplay(sender, targetName, displayDuration + " (IP)");
        }

        return true;
    }

    private void showAdminDisplay(CommandSender sender, String playerName, String duration) {
        if (sender.hasPermission("crimisonbann.admin")) {
            String display = "<red>[CrimsonBann]</red> <white>...</white> <red>[" + duration + "]</red>";
            sender.sendActionBar(MINI_MESSAGE.deserialize(display));
        }
    }
}
