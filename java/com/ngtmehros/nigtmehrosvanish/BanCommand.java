package com.ngtmehros.nigtmehrosvanish;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class BanCommand implements CommandExecutor {

    private final BanManager banManager;

    public BanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(BanManager.BAN_PERMISSION)) {
            sender.sendMessage(banManager.prefix("Du hast dafuer keine Berechtigung."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(banManager.prefix("Nutze /ban <spieler> <dauer|perm> <grund>"));
            return true;
        }

        String targetName = args[0];
        String duration = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String senderName = sender.getName();

        String error = banManager.ban(targetName, senderName, duration, reason);
        if (error != null) {
            sender.sendMessage(banManager.prefix(error));
            return true;
        }

        sender.sendMessage(banManager.prefix("Spieler " + targetName + " wurde erfolgreich gebannt."));
        return true;
    }
}
