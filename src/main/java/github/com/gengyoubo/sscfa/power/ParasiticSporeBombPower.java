package github.com.gengyoubo.sscfa.power;

import com.google.gson.JsonObject;
import github.com.gengyoubo.sscfa.balance.BalanceIntegration;
import github.com.gengyoubo.sscfa.balance.BalanceReader;
import github.com.gengyoubo.sscfa.cooldown.SkillCooldowns;
import github.com.gengyoubo.sscfa.entity.InfectionSporeBombEntity;
import github.com.gengyoubo.sscfa.init.AddonEffects;
import github.com.gengyoubo.sscfa.util.FormIdentifiers;
import github.com.gengyoubo.sscfa.util.PowerUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.api.registry.SscPowerType;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/**
 * 寄生果蝠副技能 {@code my_addon:parasitic_spore_bomb}：感染孢子炸弹。
 *
 * <p>向视线方向投出一枚孢子炸弹（{@link InfectionSporeBombEntity}），触地或命中实体后爆炸并感染范围内目标。
 * 消耗种子能量，能量不足时只播放失败音效；冷却字段同统一技能（施放即起算）。</p>
 */
public final class ParasiticSporeBombPower implements SscPowerType {
    public static final ResourceLocation TYPE = new ResourceLocation("my_addon", "parasitic_spore_bomb");

    // 以下均为默认值；运行时从 balance 快照读取（abilities.parasitic_spore_bomb）
    /** 投掷物初速度（与原版雪球速度相近） */
    private static final float PROJECTILE_SPEED = 1.4f;
    /** 散布抖动 */
    private static final float PROJECTILE_DIVERGENCE = 0.5f;
    private static final int ENERGY_COST = 1;

    /** balance 快照读取（快照未初始化回退默认常量） */
    private static final BalanceReader BAL = new BalanceReader("abilities.parasitic_spore_bomb");

    @Override
    public void onKeyPressed(ServerPlayer caster, ResourceLocation powerId, JsonObject data) {
        if (!BalanceIntegration.isPlayerReady(caster)) return;
        if (!FormPowerRuntime.test(caster, caster, data.getAsJsonObject("condition"))) return;
        if (caster.hasEffect(AddonEffects.PURIFIED.get())) return;
        String id = powerId.toString();
        if (!SkillCooldowns.ready(caster, id)) return;

        // 能量检查：不足则播放失败音效
        int energyCost = BAL.i("energy_cost", ENERGY_COST);
        if (!PowerUtils.hasResource(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, energyCost)) {
            caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4f, 1.7f);
            return;
        }
        PowerUtils.changeResourceValueAndSync(caster, FormIdentifiers.BAT_PARASITIC_FRUIT_SEED_ENERGY, -energyCost);

        // 生成投掷物（贴图为史莱姆球），从眼部位置沿视线发射
        InfectionSporeBombEntity bomb = new InfectionSporeBombEntity(caster.level(), caster);
        bomb.setItem(Items.SLIME_BALL.getDefaultInstance());
        bomb.setOwner(caster);
        bomb.setPosRaw(caster.getX(), caster.getEyeY() - 0.1, caster.getZ());
        bomb.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0.0f,
                (float) BAL.d("projectile_speed", PROJECTILE_SPEED),
                (float) BAL.d("projectile_divergence", PROJECTILE_DIVERGENCE));
        // 叠加投掷者的移动速度（落地时不带竖直分量）
        Vec3 ownerVel = caster.getDeltaMovement();
        bomb.setDeltaMovement(bomb.getDeltaMovement().add(ownerVel.x, caster.onGround() ? 0.0 : ownerVel.y, ownerVel.z));
        caster.level().addFreshEntity(bomb);

        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 1.6f);

        SkillCooldowns.instant(caster, id);
    }
}
