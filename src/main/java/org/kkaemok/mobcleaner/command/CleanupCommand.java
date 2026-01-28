package org.kkaemok.mobcleaner.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.kkaemok.mobcleaner.manager.MobManager;

public class CleanupCommand implements CommandExecutor {

    private final MobManager mobManager;

    public CleanupCommand(MobManager mobManager) {
        this.mobManager = mobManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("mobcleaner.admin")) {
            sender.sendMessage(Component.text("권한이 없습니다.", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("몹 정리를 시작합니다...", NamedTextColor.YELLOW));
        // 수동 실행 플래그 true
        mobManager.cleanMobs(true);
        return true;
    }
}