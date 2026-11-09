package github.com.gengyoubo.sscfa.state;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 月织蛛模式状态注册占位实现：按玩家 UUID 保存组件。 */
public final class RegSpiderMoonWeaverStateComponent {
    public static final Holder SPIDER_MOON_WEAVER_STATE = new Holder();

    private RegSpiderMoonWeaverStateComponent() {}

    public static final class Holder {
        private final Map<UUID, SpiderMoonWeaverStateComponent> states = new ConcurrentHashMap<>();

        public SpiderMoonWeaverStateComponent get(Player player) {
            return states.computeIfAbsent(player.getUUID(), ignored -> new SpiderMoonWeaverStateComponent());
        }
    }
}
