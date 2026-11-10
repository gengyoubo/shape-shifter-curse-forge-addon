package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 效果效率降低 {@code ssc_addon:effect_efficiency_reduction}：标记型能力。
 *
 * <p>拥有且 condition 满足的玩家，获得的非瞬时药水效果会被 {@code SscAddonLivingEntityMixin} 降级并缩短；
 * 本类只负责回答「该实体现在有没有这个能力」。</p>
 */
public final class EffectEfficiencyReductionPower implements SscPowerType {
    public static final ResourceLocation TYPE = new ResourceLocation("ssc_addon", "effect_efficiency_reduction");

    private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

    /** 该实体当前是否拥有生效中的本能力。 */
    public static boolean isActive(Entity entity) {
        return ACTIVE.contains(entity.getUUID());
    }

    @Override
    public void tick(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
        if (FormPowerRuntime.test(player, player, data.getAsJsonObject("condition"))) ACTIVE.add(player.getUUID());
        else ACTIVE.remove(player.getUUID());
    }

    @Override
    public void onRemoved(ServerPlayer player, ResourceLocation powerId, JsonObject data) {
        ACTIVE.remove(player.getUUID());
    }
}
