package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnbanCommand implements org.bukkit.command.CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public UnbanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.unban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /unban <spieler></red>"));
            return true;
        }

        String targetName = args[0];
        String unbannedBy = sender instanceof Player player ? player.getName() : "Konsole";

        if (banManager.unban(targetName, unbannedBy)) {
            String message = "<green>Spieler <yellow>" + targetName + "</yellow> wurde erfolgreich entbannt.</green>";
            sender.sendMessage(MINI_MESSAGE.deserialize(message));
        } else {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Dieser Spieler ist nicht gebannt.</red>"));
        }

        return true;
    }
}
