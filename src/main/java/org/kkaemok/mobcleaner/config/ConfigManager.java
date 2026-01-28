package org.kkaemok.mobcleaner.config;

import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {
    private final JavaPlugin plugin;
    private int intervalMinutes;
    private boolean broadcastMessage;
    private final Set<EntityType> ignoredTypes = new HashSet<>();
    private boolean ignoreAll = false;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        // config.yml이 없으면 생성, 있으면 로드
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        this.intervalMinutes = plugin.getConfig().getInt("cleanup-interval-minutes", 15);
        this.broadcastMessage = plugin.getConfig().getBoolean("broadcast-message", true);

        // 설정 파일에서 리스트 읽기
        List<String> ignoredList = plugin.getConfig().getStringList("ignored-mobs");

        ignoredTypes.clear();
        ignoreAll = false;

        // 만약 설정 파일이 비어있다면, 기본적으로 보호해야 할 몹들을 추가
        if (ignoredList.isEmpty()) {
            ignoredList = List.of("WITHER", "ENDER_DRAGON", "ELDER_GUARDIAN", "SHULKER", "VEX", "WARDEN");
            // 실제 config.yml 파일에도 기본값을 써주고 싶다면 아래 주석을 해제하세요.
            // plugin.getConfig().set("ignored-mobs", ignoredList);
            // plugin.saveConfig();
        }

        for (String typeName : ignoredList) {
            // ALL 키워드가 있으면 모든 몹 무시(기능 정지)
            if (typeName.equalsIgnoreCase("ALL")) {
                ignoreAll = true;
                break;
            }

            try {
                ignoredTypes.add(EntityType.valueOf(typeName.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                // 사용자가 오타를 냈을 경우 콘솔에 알림
                plugin.getLogger().warning("[MobCleaner] 알 수 없는 엔티티 타입 무시됨: " + typeName);
            }
        }
    }

    public int getIntervalMinutes() {
        // 주기가 0 이하일 경우 최소 1분으로 방어
        return Math.max(1, intervalMinutes);
    }

    public boolean isBroadcastMessage() {
        return broadcastMessage;
    }

    public boolean isIgnored(EntityType type) {
        // ALL이 켜져있거나, 무시 목록에 포함된 몹이면 true 반환
        return ignoreAll || ignoredTypes.contains(type);
    }
}