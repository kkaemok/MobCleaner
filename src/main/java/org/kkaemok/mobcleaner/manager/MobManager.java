package org.kkaemok.mobcleaner.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Raid;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.kkaemok.mobcleaner.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class MobManager {

    private final ConfigManager configManager;
    private final Logger logger;

    public MobManager(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    public void cleanMobs(boolean isManual) {
        int totalRemoved = 0;
        Map<EntityType, Integer> stats = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            // world.getEntities() 보다 살아있는 생물만 타겟팅하는 getLivingEntities()가 더 효율적입니다.
            for (LivingEntity entity : world.getLivingEntities()) {

                if (!(entity instanceof Monster mob)) continue;

                // 1. 이름표 보호 (최우선)
                if (mob.getCustomName() != null) continue;

                // 2. 설정 파일(config) 제외 목록 확인
                if (configManager.isIgnored(mob.getType())) continue;

                // 3. 대미지 확인 (최근 5초 이내 피해를 입었다면 보호)
                EntityDamageEvent lastDamage = mob.getLastDamageCause();
                if (lastDamage != null) {
                    if (mob.getTicksLived() - lastDamage.getEntity().getTicksLived() < 100) continue;
                }

                // 4. 습격(Raid) 보호 로직
                if (mob instanceof Raider raider) {
                    Raid raid = raider.getRaid();
                    if (raid != null) {
                        String statusName = raid.getStatus().name();
                        if (statusName.equals("ONGOING") || statusName.equals("STARTING")) continue;
                    }
                }

                // 5. [핵심] 몹의 개별 인식 범위를 고려한 플레이어 체크
                // 이 조건을 통과하지 못하면(주변에 유효한 플레이어가 없으면) 삭제됩니다.
                if (shouldProtectByFollowRange(mob)) continue;

                // 모든 보호 조건을 피했다면 삭제
                EntityType type = mob.getType();
                mob.remove();
                totalRemoved++;
                stats.put(type, stats.getOrDefault(type, 0) + 1);
            }
        }

        printResults(totalRemoved, stats, isManual);
    }

    /**
     * 몹의 'Follow Range' 속성을 읽어와서 주변 플레이어와의 거리를 비교합니다.
     */
    private boolean shouldProtectByFollowRange(Monster mob) {
        // 몹의 실제 인식 거리 속성을 가져옵니다 (좀비는 보통 35, 일반몹은 16)
        AttributeInstance followAttr = mob.getAttribute(Attribute.FOLLOW_RANGE);
        double followRange = (followAttr != null) ? followAttr.getValue() : 16.0;
        double followRangeSq = followRange * followRange;

        for (Player p : mob.getWorld().getPlayers()) {
            // 크리에이티브나 스펙테이터 모드 플레이어는 몹이 인식하지 못하므로 없는 셈 칩니다.
            if (p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;

            double distanceSq = p.getLocation().distanceSquared(mob.getLocation());

            // 1. 플레이어가 몹의 개별 인식 범위(Follow Range) 안에 있는가?
            if (distanceSq <= followRangeSq) {

                // 2. [안전장치] 인식 범위 안이더라도 최소 16블록(16^2=256) 이내라면
                // 눈앞에서 갑자기 사라지는 걸 방지하기 위해 무조건 보호합니다.
                if (distanceSq < 256) return true;

                // 3. 16블록보다는 멀지만 인식 범위 안이라면,
                // 실제로 이 플레이어를 노리고(Target) 있을 때만 보호합니다.
                if (mob.getTarget() != null && mob.getTarget().equals(p)) return true;
            }
        }

        // 주변에 크리에이티브 플레이어만 있거나, 인식 범위 밖에 있다면 false(삭제 대상)를 반환합니다.
        return false;
    }

    private void printResults(int totalRemoved, Map<EntityType, Integer> stats, boolean isManual) {
        if (totalRemoved > 0) {
            StringBuilder statMsg = new StringBuilder();
            stats.forEach((type, count) -> statMsg.append(type.name()).append(": ").append(count).append(", "));
            String detailLog = statMsg.length() > 2 ? statMsg.substring(0, statMsg.length() - 2) : statMsg.toString();

            logger.info("몹 정리 완료: " + totalRemoved + "마리 (" + detailLog + ")");

            if (configManager.isBroadcastMessage() || isManual) {
                Bukkit.broadcast(Component.text("[MobCleaner] ", NamedTextColor.GOLD)
                        .append(Component.text(totalRemoved + "마리의 몹을 정리했습니다.", NamedTextColor.WHITE)));

                if (isManual) {
                    Bukkit.broadcast(Component.text("상세: " + detailLog, NamedTextColor.GRAY), "mobcleaner.admin");
                }
            }
        } else if (isManual) {
            Bukkit.broadcast(Component.text("[MobCleaner] 정리할 몹이 없습니다.", NamedTextColor.GREEN));
        }
    }
}