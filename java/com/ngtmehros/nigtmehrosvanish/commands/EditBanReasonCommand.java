package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;

public class EditBanReasonCommand implements CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public EditBanReasonCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.ban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /editbanreason <spieler> <neuer grund></red>"));
            return true;
        }

        String targetName = args[0];
        String newReason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler nicht gefunden.</red>"));
            return true;
        }

        if (banManager.editBan(target.getUniqueId(), newReason, null)) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Bann-Grund für <yellow>" + targetName + "</yellow> wurde geändert.</green>"));
        } else {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Dieser Spieler ist nicht gebannt.</red>"));
        }

        return true;
    }
}
