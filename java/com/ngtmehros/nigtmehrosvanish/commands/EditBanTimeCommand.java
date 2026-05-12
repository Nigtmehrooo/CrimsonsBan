package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;

public class EditBanTimeCommand implements CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanManager banManager;

    public EditBanTimeCommand(BanManager banManager) {
        this.banManager = banManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.ban")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Verwendung: /editbantime <spieler> <neue dauer|perm></red>"));
            sender.sendMessage(MINI_MESSAGE.deserialize("<gray>Dauer-Beispiele: 30m, 12h, 7d oder perm</gray>"));
            return true;
        }

        String targetName = args[0];
        String durationInput = args[1];

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Spieler nicht gefunden.</red>"));
            return true;
        }

        BanManager.ParsedDuration parsedDuration = BanManager.parseDurationStatic(durationInput);
        if (!parsedDuration.valid()) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Ungültige Dauer. Nutze z.B. 30m, 12h, 7d oder perm.</red>"));
            return true;
        }

        long now = System.currentTimeMillis();
        long newExpiresAt = parsedDuration.permanent() ? 0L : now + parsedDuration.durationMillis();

        if (banManager.editBan(target.getUniqueId(), null, newExpiresAt)) {
            String displayDuration = parsedDuration.permanent() ? "Permanent" : durationInput;
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>Bann-Dauer für <yellow>" + targetName + "</yellow> wurde zu <yellow>" + displayDuration + "</yellow> geändert.</green>"));
        } else {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Dieser Spieler ist nicht gebannt.</red>"));
        }

        return true;
    }
}
