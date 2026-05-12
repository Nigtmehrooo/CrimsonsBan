package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

public class TempBanCommand implements org.bukkit.command.CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public TempBanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.ban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /tempban <spieler> <dauer> <grund></red>"));
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Dauer-Beispiele: 30m, 12h, 7d</gray>"));
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));

        String bannedBy = sender instanceof Player player ? player.getName() : "Konsole";
        String result = banManager.ban(targetName, bannedBy, duration, reason);

        if (result != null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>" + result + "</red>"));
        } else {
            String message = "<green>Spieler <yellow>" + targetName + "</yellow> wurde für <yellow>" + duration + "</yellow> gebannt.</green>";
            sender.sendMessage(MINI_MESSAGE.deserialize(message));
            
            // Offline-Spieler kicken falls online
            Player onlineTarget = Bukkit.getPlayer(targetName);
            if (onlineTarget != null) {
                onlineTarget.kick(net.kyori.adventure.text.Component.text("Du wurdest gebannt! Grund: " + reason));
            }
            
            // Show admin display
            showAdminDisplay(sender, targetName, duration);
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
