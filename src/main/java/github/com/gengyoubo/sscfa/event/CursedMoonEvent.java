package github.com.gengyoubo.sscfa.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * 诅咒之月对某个服务端玩家开?结束生效，在 {@code MinecraftForge.EVENT_BUS} 上发出? *
 * <p>?SSC Forge 玩家数据里的「诅咒之月已生效」标记为准：SSC 每次诅咒之月只对每名玩家应用一次开始效? * （无论形态是否免疫诅咒之月），因?{@link Begin} 每次诅咒之月每人最多一次。登录时只记录状态，不发事件? * 带着已生效标记重进不会重复触发。只用于观察，不可取消?/p>
 */
public class CursedMoonEvent extends PlayerEvent {
    protected CursedMoonEvent(ServerPlayer player) {
        super(player);
    }

    public ServerPlayer getServerPlayer() {
        return (ServerPlayer) getEntity();
    }

    /** SSC 刚对该玩家应用了诅咒之月开始效果（提示、可能的强制变形之后）?*/
    public static final class Begin extends CursedMoonEvent {
        public Begin(ServerPlayer player) {
            super(player);
        }
    }

    /** 该玩家的诅咒之月已结束（SSC 清除了已生效标记）?*/
    public static final class End extends CursedMoonEvent {
        public End(ServerPlayer player) {
            super(player);
        }
    }
}
