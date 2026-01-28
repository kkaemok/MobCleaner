package org.kkaemok.mobcleaner;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.kkaemok.mobcleaner.command.CleanupCommand;
import org.kkaemok.mobcleaner.config.ConfigManager;
import org.kkaemok.mobcleaner.manager.MobManager;

import java.util.Objects;

public final class Mobcleaner extends JavaPlugin {

    private ConfigManager configManager;
    private MobManager mobManager;
    private BukkitRunnable cleanTask;

    @Override
    public void onEnable() {
        // 1. Config 로드
        this.configManager = new ConfigManager(this);

        // 2. Manager 초기화
        this.mobManager = new MobManager(configManager, getLogger());

        // 3. 커맨드 등록
        if (getCommand("몹정리") != null) {
            Objects.requireNonNull(getCommand("몹정리")).setExecutor(new CleanupCommand(mobManager));
        }

        // 4. 스케줄러 시작
        startScheduler();

        getLogger().info("Mobcleaner plugin enabled! (Interval: " + configManager.getIntervalMinutes() + " min)");
    }

    @Override
    public void onDisable() {
        if (cleanTask != null && !cleanTask.isCancelled()) {
            cleanTask.cancel();
        }
        getLogger().info("Mobcleaner plugin disabled.");
    }

    private void startScheduler() {
        long intervalTicks = configManager.getIntervalMinutes() * 60 * 20L;

        cleanTask = new BukkitRunnable() {
            @Override
            public void run() {
                mobManager.cleanMobs(false); // 자동 실행
            }
        };
        cleanTask.runTaskTimer(this, intervalTicks, intervalTicks);
    }
}