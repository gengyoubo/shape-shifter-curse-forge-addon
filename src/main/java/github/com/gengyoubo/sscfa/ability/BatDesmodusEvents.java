package github.com.gengyoubo.sscfa.ability;

import github.com.gengyoubo.sscfa.SscAddonForge;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.event.PlayerSessionCleanup;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.FormUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * 吸血蝙蝠血渴值系统与游戏事件的接线（原 Fabric 版散在 LivingEntity mixin 与服务端事件中心里）：
 * <ul>
 *   <li>{@link LivingAttackEvent}（伤害流程开头）：蝙蝠玩家造成或受到伤害时标记战斗；</li>
 *   <li>{@link LivingHurtEvent} 高优先级（拿到原始伤害值）：普攻命中累积血渴、按阶段吸血；</li>
 *   <li>{@link LivingHurtEvent} 普通优先级（伤害结算前）：0-25 阶段受伤 ×0.85、75-100 阶段造成伤害 ×1.15；</li>
 *   <li>每个服务端玩家 tick：脱战衰减、饰品效果；离线与停服清状态。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = SscAddonForge.MOD_ID)
public final class BatDesmodusEvents {
    private static final BalanceReader BAL = new BalanceReader("abilities.bat_blood_thirst");

    static {
        PlayerSessionCleanup.register((connection, server) -> BatDesmodusBloodThirst.clearPlayer(connection.player.getUUID()));
    }

    private BatDesmodusEvents() {}

    private static boolean isBat(Entity entity) {
        return entity instanceof ServerPlayer player && FormUtils.isForm(player, FormIdentifiers.BAT_DESMODUS);
    }

    /** 蝙蝠玩家造成或受到（他人的）伤害时标记战斗。 */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide()) return;
        Entity attacker = event.getSource().getEntity();
        if (isBat(attacker)) BatDesmodusBloodThirst.markCombat((ServerPlayer) attacker);
        if (attacker != null && attacker != self && isBat(self)) BatDesmodusBloodThirst.markCombat((ServerPlayer) self);
    }

    /**
     * 伤害确定生效后：普攻命中累积血渴（受白名单与 0.3s 内部冷却约束），
     * 并按阶段吸血（50-75 → 30%、75-100 → 52.5%，戴嗜血指环再 +15%）。吸血按原始伤害值计算。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHitDealt(LivingHurtEvent event) {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide()) return;
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer attacker) || attacker == self || !isBat(attacker)) return;
        float amount = event.getAmount();

        // 普攻命中：只算玩家近战（player_attack），排除魔法、间接魔法、起爆范围伤害等
        boolean melee = source.is(DamageTypes.PLAYER_ATTACK);
        if (melee) BatDesmodusBloodThirst.onAttackHit(attacker, self);

        // 吸血：玩家亲手造成的近战 / 起爆范围伤害（排除环境与间接伤害）
        if (melee || (source.getDirectEntity() == attacker
                && !source.is(DamageTypes.INDIRECT_MAGIC)
                && !source.is(DamageTypes.FELL_OUT_OF_WORLD)
                && !source.is(DamageTypes.GENERIC_KILL))) {
            int stage = BatDesmodusBloodThirst.getStage(attacker);
            float rate = 0f;
            if (stage == 2) rate = (float) BAL.d("lifesteal_stage2", 0.30);
            else if (stage == 3) rate = (float) BAL.d("lifesteal_stage3", 0.525);
            if (rate > 0f && BatDesmodusBloodThirst.hasBloodlustRing(attacker)) rate += (float) BAL.d("ring_bonus", 0.15);
            if (rate > 0f && amount > 0f) attacker.heal(amount * rate);
        }
    }

    /** 血渴阶段修正伤害：受害方蝙蝠 0-25 阶段 ×0.85；攻击方蝙蝠 75-100 阶段 ×1.15（光环等被动伤害不加成）。 */
    @SubscribeEvent
    public static void onHurtScale(LivingHurtEvent event) {
        LivingEntity self = event.getEntity();
        if (self.level().isClientSide()) return;
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        if (isBat(self) && !source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)
                && !source.is(DamageTypes.INDIRECT_MAGIC) && BatDesmodusBloodThirst.getStage((ServerPlayer) self) == 0) {
            amount *= (float) BAL.d("stage0_taken_mul", 0.85);
        }
        Entity attacker = source.getEntity();
        if (attacker != self && isBat(attacker) && !BatDesmodusBloodThirst.SUPPRESS_OUTGOING_BUFF.get()
                && BatDesmodusBloodThirst.getStage((ServerPlayer) attacker) == 3) {
            amount *= (float) BAL.d("stage3_out_mul", 1.15);
        }
        if (amount != event.getAmount()) event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER && event.player instanceof ServerPlayer player) {
            BatDesmodusBloodThirst.tick(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BatDesmodusBloodThirst.clearAll();
    }
}
