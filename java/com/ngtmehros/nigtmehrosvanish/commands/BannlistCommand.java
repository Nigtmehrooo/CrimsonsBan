package com.ngtmehros.nigtmehrosvanish.commands;

import com.ngtmehros.nigtmehrosvanish.BanListGUI;
import com.ngtmehros.nigtmehrosvanish.BanManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BannlistCommand implements org.bukkit.command.CommandExecutor {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private final BanListGUI banListGUI;

    public BannlistCommand(BanManager banManager, BanListGUI banListGUI) {
        this.banListGUI = banListGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.hasPermission("crimisonbann.bannlist")) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Du hast keine Berechtigung für diesen Befehl.</red>"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(MINI_MESSAGE.deserialize("<red>Dieser Befehl kann nur von Spielern verwendet werden.</red>"));
            return true;
        }

        banListGUI.openBanList(player);
        return true;
    }
}
