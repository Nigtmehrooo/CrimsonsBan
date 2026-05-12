package com.ngtmehros.nigtmehrosvanish;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class UnbanCommand implements CommandExecutor {

    private final BanManager banManager;

    public UnbanCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(BanManager.UNBAN_PERMISSION)) {
            sender.sendMessage(banManager.prefix("Du hast dafuer keine Berechtigung."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(banManager.prefix("Nutze /unban <spieler>"));
            return true;
        }

        boolean removed = banManager.unban(args[0], sender.getName());
        if (!removed) {
            sender.sendMessage(banManager.prefix("Fuer diesen Spieler liegt kein aktiver Bann vor."));
            return true;
        }

        sender.sendMessage(banManager.prefix("Spieler " + args[0] + " wurde entbannt."));
        return true;
    }
}
