package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;

public class BanCommand implements org.bukkit.command.CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public BanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.ban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /ban <spieler> <grund></red>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        String bannedBy = sender instanceof Player player ? player.getName() : "Konsole";
        String result = banManager.ban(targetName, bannedBy, "perm", reason);

        if (result != null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>" + result + "</red>"));
        } else {
            String duration = "Permanent";
            String message = "<green>Spieler <yellow>" + targetName + "</yellow> wurde erfolgreich gebannt.</green>";
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
